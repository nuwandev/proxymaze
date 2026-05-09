package com.binarybeasts.store;

import com.binarybeasts.domain.Alert;
import com.binarybeasts.domain.ProxyNode;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class InMemoryStateStore {

    private final ConcurrentHashMap<String, ProxyNode> proxyPool = new ConcurrentHashMap<>();

    private final List<Alert> alertHistory = Collections.synchronizedList(new ArrayList<>());

    private final AtomicReference<Alert> activeAlert = new AtomicReference<>(null);

    private final AtomicLong alertIdCounter = new AtomicLong(1);

    private final AtomicLong totalChecksPerformed = new AtomicLong(0);
    private final AtomicLong totalWebhookDeliveries = new AtomicLong(0);

    public void addProxy(ProxyNode node) {
        proxyPool.put(node.getId(), node);
    }

    public void clearPool() {
        proxyPool.clear();
        // NOTE: do NOT clear alertHistory here. That's the rule.
    }

    public Map<String, ProxyNode> getPool() {
        return Collections.unmodifiableMap(proxyPool);
    }

    public Optional<ProxyNode> findProxy(String id) {
        return Optional.ofNullable(proxyPool.get(id));
    }

    public Collection<ProxyNode> getAllProxies() {
        return proxyPool.values();
    }

    public String nextAlertId() {
        return "alert-" + alertIdCounter.getAndIncrement();
    }

    public Alert getActiveAlert() {
        return activeAlert.get();
    }

    public void setActiveAlert(Alert alert) {
        activeAlert.set(alert);
        if (alert != null) alertHistory.add(alert);
    }

    public void clearActiveAlert() {
        activeAlert.set(null);
    }

    public List<Alert> getAllAlerts() {
        return Collections.unmodifiableList(alertHistory);
    }

    public void incrementTotalChecks(long count) {
        totalChecksPerformed.addAndGet(count);
    }

    public long getTotalChecks() {
        return totalChecksPerformed.get();
    }

    public void incrementWebhookDeliveries() {
        totalWebhookDeliveries.incrementAndGet();
    }

    public long getTotalWebhookDeliveries() {
        return totalWebhookDeliveries.get();
    }
}