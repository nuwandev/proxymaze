package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MetricsResponse(
        @JsonProperty("total_checks") long totalChecks,
        @JsonProperty("current_pool_size") int currentPoolSize,
        @JsonProperty("active_alerts") int activeAlerts,
        @JsonProperty("total_alerts") int totalAlerts,
        @JsonProperty("webhook_deliveries") long webhookDeliveries
) {
}

