package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RuntimeConfigResponse(
        @JsonProperty("check_interval_seconds") int checkIntervalSeconds,
        @JsonProperty("request_timeout_ms") int requestTimeoutMs
) {
}

