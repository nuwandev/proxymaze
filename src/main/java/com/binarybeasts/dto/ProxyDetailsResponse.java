package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record ProxyDetailsResponse(
        @JsonProperty("id") String id,
        @JsonProperty("url") String url,
        @JsonProperty("status") String status,
        @JsonProperty("last_checked_at") Instant lastCheckedAt,
        @JsonProperty("consecutive_failures") int consecutiveFailures,
        @JsonProperty("total_checks") long totalChecks,
        @JsonProperty("uptime_percentage") double uptimePercentage,
        @JsonProperty("history") List<ProxyCheckRecordResponse> history
) {
}

