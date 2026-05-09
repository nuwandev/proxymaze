package com.binarybeasts.service;

import com.binarybeasts.domain.Alert;
import com.binarybeasts.domain.ProxyNode;
import com.binarybeasts.engine.ProxyProbeClient;
import com.binarybeasts.engine.ProbeOutcome;
import com.binarybeasts.store.InMemoryStateStore;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class MonitoringService {
    private final InMemoryStateStore store;
    private final ProxyProbeClient probeClient;
    private final AlertService alertService;
    private final ReentrantLock cycleLock = new ReentrantLock();
    private final Clock clock = Clock.systemUTC();

    public MonitoringService(InMemoryStateStore store, ProxyProbeClient probeClient, AlertService alertService) {
        this.store = store;
        this.probeClient = probeClient;
        this.alertService = alertService;
    }

    public void runMonitoringCycle() {
        if (!cycleLock.tryLock()) {
            return;
        }

        try {
            List<ProxyNode> proxies = store.snapshotProxies();
            int timeoutMs = Math.max(1, store.getRuntimeConfig().getRequestTimeoutMs());

            for (ProxyNode proxy : proxies) {
                Instant checkedAt = Instant.now(clock);
                ProbeOutcome outcome = probeClient.probe(proxy.getUrl(), timeoutMs);
                store.recordProbe(proxy.getId(), outcome, checkedAt);
            }

            Instant evaluationTime = Instant.now(clock);
            double failureRate = store.getFailureRate();
            int totalProxies = store.getCurrentPoolSize();
            int failedProxies = store.getDownCount();
            List<String> failedProxyIds = store.getFailedProxyIds();
            alertService.evaluate(failureRate, totalProxies, failedProxies, failedProxyIds, evaluationTime);
        } finally {
            cycleLock.unlock();
        }
    }

    public List<ProxyNode> snapshotProxies() {
        return store.snapshotProxies();
    }

    public Optional<ProxyNode> findProxy(String id) {
        return store.findProxy(id);
    }

    public List<Alert> getAlerts() {
        return alertService.getAlerts();
    }
}

