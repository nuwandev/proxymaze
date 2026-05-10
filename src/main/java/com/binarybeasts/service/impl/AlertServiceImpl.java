package com.binarybeasts.service.impl;

import com.binarybeasts.domain.Alert;
import com.binarybeasts.domain.ProxyNode;
import com.binarybeasts.domain.ProxyStatus;
import com.binarybeasts.service.AlertService;
import com.binarybeasts.service.WebhookService;
import com.binarybeasts.store.InMemoryStateStore;
import com.binarybeasts.util.LogHighlighter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertServiceImpl implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertServiceImpl.class);

    private static final double THRESHOLD = 0.20;

    private final InMemoryStateStore store;

    @Autowired(required = false) // set to true after implementing WebhookService
    private WebhookService webhookService;

    public AlertServiceImpl(InMemoryStateStore store) {
        this.store = store;
    }

    @Override
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

        LogHighlighter.debug(log, "Alert", "Evaluated - failureRate={:.2f}%, threshold=20%, activeAlert={}",
                failureRate * 100,
                store.getActiveAlert() != null ? store.getActiveAlert().getAlertId() : "none");

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

        LogHighlighter.warn(log, "Alert", "FIRED - id={}, failureRate={:.2f}%, down={}/{}, failedIds={}",
                alert.getAlertId(),
                failureRate * 100,
                downCount,
                checked.size(),
                failedIds);

        if (webhookService != null) {
            webhookService.enqueue(alert, "alert.fired");
        }
    }

    private void resolveAlert(Alert alert) {
        alert.resolve(Instant.now());
        store.clearActiveAlert();

        LogHighlighter.info(log, "Alert", "RESOLVED - id={}, resolvedAt={}", alert.getAlertId(), alert.getResolvedAt());

        if (webhookService != null) {
            webhookService.enqueue(alert, "alert.resolved");
        }
    }

    @Override
    public List<Alert> getAllAlerts() {
        return store.getAllAlerts();
    }

    @Override
    public Alert getActiveAlert() {
        return store.getActiveAlert();
    }
}