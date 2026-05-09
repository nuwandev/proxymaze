package com.binarybeasts.store;

import com.binarybeasts.domain.Alert;
import com.binarybeasts.domain.ProxyCheckRecord;
import com.binarybeasts.domain.ProxyNode;
import com.binarybeasts.domain.ProxyStatus;
import com.binarybeasts.domain.RuntimeConfig;
import com.binarybeasts.engine.ProbeOutcome;
import com.binarybeasts.service.MetricsSnapshot;
import com.binarybeasts.util.ProxyIdExtractor;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryStateStore {
    private final ConcurrentHashMap<String, ProxyNode> proxies = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Alert> alerts = new CopyOnWriteArrayList<>();
    private final RuntimeConfig runtimeConfig = new RuntimeConfig();
    private final AtomicLong alertSequence = new AtomicLong(0);
    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong webhookDeliveries = new AtomicLong(0);
    private final Object mutationLock = new Object();
    private volatile Alert activeAlert;

    public RuntimeConfig getRuntimeConfig() {
        return runtimeConfig;
    }

    public void updateRuntimeConfig(int checkIntervalSeconds, int requestTimeoutMs) {
        synchronized (mutationLock) {
            runtimeConfig.setCheckIntervalSeconds(checkIntervalSeconds);
            runtimeConfig.setRequestTimeoutMs(requestTimeoutMs);
        }
    }

    public List<ProxyNode> replaceProxies(Collection<String> urls) {
        synchronized (mutationLock) {
            proxies.clear();
            return addProxiesLocked(urls);
        }
    }

    public List<ProxyNode> addProxies(Collection<String> urls) {
        synchronized (mutationLock) {
            return addProxiesLocked(urls);
        }
    }

    public void clearProxies() {
        synchronized (mutationLock) {
            proxies.clear();
        }
    }

    public Optional<ProxyNode> findProxy(String id) {
        return Optional.ofNullable(proxies.get(id));
    }

    public List<ProxyNode> snapshotProxies() {
        List<ProxyNode> snapshot = new ArrayList<>(proxies.values());
        snapshot.sort(Comparator.comparing(ProxyNode::getId));
        return snapshot;
    }

    public List<ProxyCheckRecord> getProxyHistory(String id) {
        ProxyNode node = proxies.get(id);
        return node == null ? List.of() : node.getHistory();
    }

    public ProxyNode recordProbe(String id, ProbeOutcome outcome, Instant checkedAt) {
        ProxyNode node = proxies.get(id);
        if (node == null) {
            return null;
        }
        if (outcome.status() == ProxyStatus.UP) {
            node.recordSuccess(checkedAt);
        } else {
            node.recordFailure(checkedAt);
        }
        totalChecks.incrementAndGet();
        return node;
    }

    public int getCurrentPoolSize() {
        return proxies.size();
    }

    public int getUpCount() {
        int count = 0;
        for (ProxyNode proxy : proxies.values()) {
            if (proxy.getStatus() == ProxyStatus.UP) {
                count++;
            }
        }
        return count;
    }

    public int getDownCount() {
        int count = 0;
        for (ProxyNode proxy : proxies.values()) {
            if (proxy.getStatus() == ProxyStatus.DOWN) {
                count++;
            }
        }
        return count;
    }

    public double getFailureRate() {
        int total = getCurrentPoolSize();
        if (total == 0) {
            return 0.0;
        }
        return (double) getDownCount() / total;
    }

    public List<String> getFailedProxyIds() {
        return proxies.values().stream()
                .filter(proxy -> proxy.getStatus() == ProxyStatus.DOWN)
                .map(ProxyNode::getId)
                .sorted()
                .toList();
    }

    public Optional<Alert> getActiveAlert() {
        return Optional.ofNullable(activeAlert);
    }

    public Alert fireAlert(double failureRate, int totalProxies, int failedProxies, List<String> failedProxyIds, Instant firedAt) {
        synchronized (mutationLock) {
            if (activeAlert != null && activeAlert.getStatus() == com.binarybeasts.domain.AlertStatus.ACTIVE) {
                return activeAlert;
            }
            Alert alert = new Alert(
                    "alert-" + alertSequence.incrementAndGet(),
                    failureRate,
                    totalProxies,
                    failedProxies,
                    failedProxyIds,
                    firedAt
            );
            activeAlert = alert;
            alerts.add(alert);
            return alert;
        }
    }

    public Optional<Alert> resolveActiveAlert(Instant resolvedAt) {
        synchronized (mutationLock) {
            if (activeAlert == null) {
                return Optional.empty();
            }
            activeAlert.resolve(resolvedAt);
            Alert resolved = activeAlert;
            activeAlert = null;
            return Optional.of(resolved);
        }
    }

    public List<Alert> getAlerts() {
        return List.copyOf(alerts);
    }

    public MetricsSnapshot snapshotMetrics() {
        return new MetricsSnapshot(
                totalChecks.get(),
                getCurrentPoolSize(),
                activeAlert == null ? 0 : 1,
                alerts.size(),
                webhookDeliveries.get()
        );
    }

    public long incrementWebhookDeliveries() {
        return webhookDeliveries.incrementAndGet();
    }

    private List<ProxyNode> addProxiesLocked(Collection<String> urls) {
        List<ProxyNode> added = new ArrayList<>();
        for (String url : urls) {
            String id = ProxyIdExtractor.extract(url);
            ProxyNode node = new ProxyNode(id, url);
            proxies.put(id, node);
            added.add(node);
        }
        added.sort(Comparator.comparing(ProxyNode::getId));
        return added;
    }
}

