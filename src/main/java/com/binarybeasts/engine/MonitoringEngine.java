package com.binarybeasts.engine;

import com.binarybeasts.domain.ProxyNode;
import com.binarybeasts.domain.ProxyStatus;
import com.binarybeasts.domain.RuntimeConfig;
import com.binarybeasts.service.AlertService;
import com.binarybeasts.store.InMemoryStateStore;
import com.binarybeasts.util.LogHighlighter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MonitoringEngine {

    private static final Logger log = LoggerFactory.getLogger(MonitoringEngine.class);

    private final InMemoryStateStore store;
    private final AlertService alertService;
    private final RuntimeConfig config;

    private final AtomicBoolean cycleRunning = new AtomicBoolean(false);

    // Non daemon thread because keeps running on Render, won't get killed when idle on free triel
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "monitoring-engine");
                t.setDaemon(false);
                return t;
            });

    private final Object schedulerLock = new Object();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private volatile ScheduledFuture<?> currentTask;

    public MonitoringEngine(InMemoryStateStore store, AlertService alertService,
                            RuntimeConfig config) {
        this.store = store;
        this.alertService = alertService;
        this.config = config;
    }

    @PostConstruct
    public void start() {
        scheduleNext();
        LogHighlighter.info(log, "Engine", "Monitoring engine started - interval={}s, timeout={}ms",
                config.getCheckIntervalSeconds(), config.getRequestTimeoutMs());
    }

    public void reschedule() {
        synchronized (schedulerLock) {
            if (currentTask != null) currentTask.cancel(false);
            scheduleNext();
            LogHighlighter.info(log, "Engine", "Rescheduled - new interval={}s, timeout={}ms",
                    config.getCheckIntervalSeconds(), config.getRequestTimeoutMs());
        }
    }

    private void scheduleNext() {
        int interval = config.getCheckIntervalSeconds();
        // scheduleWithFixedDelay — next cycle starts AFTER previous finishes
        // safer than scheduleAtFixedRate which can overlap
        currentTask = scheduler.scheduleWithFixedDelay(
                this::runCycle, 0, interval, TimeUnit.SECONDS
        );
    }

    private void runCycle() {
        if (!cycleRunning.compareAndSet(false, true)) {
            LogHighlighter.warn(log, "Engine", "Cycle skipped - previous cycle still running");
            return;
        }

        try {
            Collection<ProxyNode> proxies = store.getAllProxies();

            if (proxies.isEmpty()) {
                LogHighlighter.debug(log, "Engine", "Cycle triggered - pool is empty, nothing to probe");
                return;
            }

            LogHighlighter.info(log, "Engine", "Cycle started - probing {} proxies", proxies.size());
            int timeout = config.getRequestTimeoutMs();
            Instant now = Instant.now();

            proxies.parallelStream().forEach(proxy -> probeProxy(proxy, timeout, now));

            store.incrementTotalChecks(proxies.size());

            long upCount = proxies.stream().filter(p -> p.getStatus() == ProxyStatus.UP).count();
            long downCount = proxies.stream().filter(p -> p.getStatus() == ProxyStatus.DOWN).count();
            long pendingCount = proxies.stream().filter(p -> p.getStatus() == ProxyStatus.PENDING).count();

            LogHighlighter.info(log, "Engine", "Cycle complete - up={}, down={}, pending={}",
                    upCount, downCount, pendingCount);

            alertService.evaluate(store.getAllProxies());

        } finally {
            cycleRunning.set(false);
        }
    }

    private void probeProxy(ProxyNode proxy, int timeoutMs, Instant now) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(proxy.getUrl()))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .GET()
                    .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            int code = response.statusCode();
            if (code >= 200 && code < 300) {
                proxy.recordSuccess(now);
                LogHighlighter.debug(log, "Probe", "{} → UP ({})", proxy.getId(), code);
            } else {
                proxy.recordFailure(now);
                LogHighlighter.warn(log, "Probe", "{} → DOWN (HTTP {})", proxy.getId(), code);
            }

        } catch (HttpTimeoutException e) {
            proxy.recordFailure(now);
            LogHighlighter.warn(log, "Probe", "{} → DOWN (timeout after {}ms)", proxy.getId(), timeoutMs);
        } catch (IOException e) {
            proxy.recordFailure(now);
            LogHighlighter.warn(log, "Probe", "{} → DOWN (connection error: {})", proxy.getId(), e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            proxy.recordFailure(now);
            LogHighlighter.warn(log, "Probe", "{} → DOWN (interrupted)", proxy.getId());
        } catch (Exception e) {
            proxy.recordFailure(now);
            LogHighlighter.error(log, "Probe", "{} → DOWN (unexpected: {})", proxy.getId(), e.getMessage());
        }
    }
}