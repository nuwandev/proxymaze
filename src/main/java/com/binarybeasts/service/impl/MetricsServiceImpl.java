package com.binarybeasts.service.impl;

import com.binarybeasts.service.MetricsService;
import com.binarybeasts.store.InMemoryStateStore;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MetricsServiceImpl implements MetricsService {

    private final InMemoryStateStore store;

    public MetricsServiceImpl(InMemoryStateStore store) {
        this.store = store;
    }

    @Override
    public Map<String, Object> getMetrics() {
        long activeAlerts = store.getActiveAlert() != null ? 1 : 0;

        return Map.of(
                "total_checks", store.getTotalChecks(),
                "current_pool_size", store.getPool().size(),
                "active_alerts", activeAlerts,
                "total_alerts", store.getAllAlerts().size(),
                "webhook_deliveries", store.getTotalWebhookDeliveries()
        );
    }
}