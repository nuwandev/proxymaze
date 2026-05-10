package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record AlertResponse(
        @JsonProperty("alert_id") String alertId,
        @JsonProperty("status") String status,
        @JsonProperty("failure_rate") double failureRate,
        @JsonProperty("total_proxies") int totalProxies,
        @JsonProperty("failed_proxies") int failedProxies,
        @JsonProperty("failed_proxy_ids") List<String> failedProxyIds,
        @JsonProperty("threshold") double threshold,
        @JsonProperty("fired_at") Instant firedAt,
        @JsonProperty("resolved_at") Instant resolvedAt,
        @JsonProperty("message") String message
) {
}

