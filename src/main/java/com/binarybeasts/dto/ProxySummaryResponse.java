package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ProxySummaryResponse(
        @JsonProperty("id") String id,
        @JsonProperty("url") String url,
        @JsonProperty("status") String status,
        @JsonProperty("last_checked_at") Instant lastCheckedAt,
        @JsonProperty("consecutive_failures") int consecutiveFailures
) {
}

