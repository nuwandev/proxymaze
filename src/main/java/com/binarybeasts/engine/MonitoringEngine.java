package com.binarybeasts.engine;

import com.binarybeasts.domain.ProxyNode;
import com.binarybeasts.domain.RuntimeConfig;
import com.binarybeasts.service.impl.AlertServiceImpl;
import com.binarybeasts.store.InMemoryStateStore;
import jakarta.annotation.PostConstruct;
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

    private final InMemoryStateStore store;
    private final AlertServiceImpl alertService;
    private final RuntimeConfig config;

    private final AtomicBoolean cycleRunning = new AtomicBoolean(false);

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private final Object schedulerLock = new Object();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private ScheduledFuture<?> currentTask;

    public MonitoringEngine(InMemoryStateStore store, AlertServiceImpl alertService,
                            RuntimeConfig config) {
        this.store = store;
        this.alertService = alertService;
        this.config = config;
    }

    @PostConstruct
    public void start() {
        scheduleNext();
    }

    // Called when config changes — reschedules with new interval
    public void reschedule() {
        synchronized (schedulerLock) {
            if (currentTask != null) currentTask.cancel(false);
            scheduleNext();
        }
    }

    private void scheduleNext() {
        int interval = config.getCheckIntervalSeconds();
        currentTask = scheduler.scheduleAtFixedRate(
                this::runCycle, 0, interval, TimeUnit.SECONDS
        );
    }

    private void runCycle() {
        if (!cycleRunning.compareAndSet(false, true)) return;

        try {
            Collection<ProxyNode> proxies = store.getAllProxies();
            if (proxies.isEmpty()) return;

            int timeout = config.getRequestTimeoutMs();
            Instant now = Instant.now();

            proxies.parallelStream().forEach(proxy -> probeProxy(proxy, timeout, now));

            store.incrementTotalChecks(proxies.size());

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
            } else {
                proxy.recordFailure(now);
            }

        } catch (HttpTimeoutException e) {
            proxy.recordFailure(now);
        } catch (IOException e) {
            proxy.recordFailure(now);  // connection refused, DNS failure, etc.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            proxy.recordFailure(now);
        } catch (Exception e) {
            proxy.recordFailure(now);
        }
    }
}