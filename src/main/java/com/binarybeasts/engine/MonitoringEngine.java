package com.binarybeasts.engine;

import com.binarybeasts.service.MonitoringService;
import com.binarybeasts.store.InMemoryStateStore;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MonitoringEngine {
    private final MonitoringService monitoringService;
    private final InMemoryStateStore store;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public MonitoringEngine(MonitoringService monitoringService, InMemoryStateStore store) {
        this.monitoringService = monitoringService;
        this.store = store;
        this.executor = Executors.newSingleThreadScheduledExecutor(new MonitoringThreadFactory());
    }

    @PostConstruct
    public void start() {
        scheduleNext(0L);
    }

    @PreDestroy
    public void stop() {
        stopped.set(true);
        executor.shutdownNow();
    }

    private void scheduleNext(long delayMs) {
        if (stopped.get()) {
            return;
        }
        executor.schedule(this::runOnceAndReschedule, Math.max(0L, delayMs), TimeUnit.MILLISECONDS);
    }

    private void runOnceAndReschedule() {
        if (stopped.get()) {
            return;
        }

        try {
            monitoringService.runMonitoringCycle();
        } finally {
            if (!stopped.get()) {
                scheduleNext(intervalMs());
            }
        }
    }

    private long intervalMs() {
        return Math.max(1, store.getRuntimeConfig().getCheckIntervalSeconds()) * 1000L;
    }

    private static final class MonitoringThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            Thread thread = new Thread(runnable, "proxymaze-monitoring-engine");
            thread.setDaemon(true);
            return thread;
        }
    }
}



