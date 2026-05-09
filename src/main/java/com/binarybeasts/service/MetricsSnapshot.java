package com.binarybeasts.service;

public record MetricsSnapshot(long totalChecks,
                              int currentPoolSize,
                              int activeAlerts,
                              int totalAlerts,
                              long webhookDeliveries) {
}

