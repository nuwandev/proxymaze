package com.binarybeasts.service;

import com.binarybeasts.domain.Alert;
import com.binarybeasts.domain.ProxyNode;
import com.binarybeasts.domain.ProxyStatus;
import com.binarybeasts.store.InMemoryStateStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertService {

    private static final double THRESHOLD = 0.20;

    private final InMemoryStateStore store;

    @Autowired(required = false)
    private WebhookService webhookService;

    public AlertService(InMemoryStateStore store) {
        this.store = store;
    }

    public void evaluate(Collection<ProxyNode> proxies) {
        List<ProxyNode> checked = proxies.stream()
                .filter(p -> p.getStatus() != ProxyStatus.PENDING)
                .collect(Collectors.toList());

        if (checked.isEmpty()) return;

        long downCount = checked.stream()
                .filter(p -> p.getStatus() == ProxyStatus.DOWN)
                .count();

        double failureRate = (double) downCount / checked.size();

        Alert currentActive = store.getActiveAlert();

        if (failureRate >= THRESHOLD) {
            if (currentActive == null) {
                fireAlert(failureRate, checked, downCount);
            }

        } else {
            if (currentActive != null) {
                resolveAlert(currentActive);
            }
        }
    }

    private void fireAlert(double failureRate, List<ProxyNode> checked, long downCount) {
        List<String> failedIds = checked.stream()
                .filter(p -> p.getStatus() == ProxyStatus.DOWN)
                .map(ProxyNode::getId)
                .collect(Collectors.toList());

        Alert alert = new Alert(
                store.nextAlertId(),
                failureRate,
                checked.size(),
                (int) downCount,
                failedIds,
                Instant.now()
        );

        store.setActiveAlert(alert);

        if (webhookService != null) {
            webhookService.enqueue(alert, "alert.fired");
        }
    }

    private void resolveAlert(Alert alert) {
        alert.resolve(Instant.now());
        store.clearActiveAlert();

        if (webhookService != null) {
            webhookService.enqueue(alert, "alert.resolved");
        }
    }

    public List<Alert> getAllAlerts() {
        return store.getAllAlerts();
    }

    public Alert getActiveAlert() {
        return store.getActiveAlert();
    }
}