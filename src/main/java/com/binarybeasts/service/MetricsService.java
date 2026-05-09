package com.binarybeasts.service;

import com.binarybeasts.store.InMemoryStateStore;

import org.springframework.stereotype.Service;

@Service
public class MetricsService {
    private final InMemoryStateStore store;

    public MetricsService(InMemoryStateStore store) {
        this.store = store;
    }

    public MetricsSnapshot snapshot() {
        return store.snapshotMetrics();
    }

    public long incrementWebhookDeliveries() {
        return store.incrementWebhookDeliveries();
    }
}

