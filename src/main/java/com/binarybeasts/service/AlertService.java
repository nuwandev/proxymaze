package com.binarybeasts.service;

import com.binarybeasts.domain.Alert;
import com.binarybeasts.store.InMemoryStateStore;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AlertService {
    public static final double THRESHOLD = 0.20;

    private final InMemoryStateStore store;

    public AlertService(InMemoryStateStore store) {
        this.store = store;
    }

    public AlertLifecycleOutcome evaluate(double failureRate, int totalProxies, int failedProxies, List<String> failedProxyIds, Instant now) {
        if (failureRate >= THRESHOLD) {
            return store.getActiveAlert()
                    .map(alert -> new AlertLifecycleOutcome(false, false, alert))
                    .orElseGet(() -> new AlertLifecycleOutcome(true, false,
                            store.fireAlert(failureRate, totalProxies, failedProxies, failedProxyIds, now)));
        }

        return store.resolveActiveAlert(now)
                .map(alert -> new AlertLifecycleOutcome(false, true, alert))
                .orElseGet(() -> new AlertLifecycleOutcome(false, false, null));
    }

    public List<Alert> getAlerts() {
        return store.getAlerts();
    }

    public java.util.Optional<Alert> getActiveAlert() {
        return store.getActiveAlert();
    }
}

