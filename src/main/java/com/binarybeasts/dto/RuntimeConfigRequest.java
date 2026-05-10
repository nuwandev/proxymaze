package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RuntimeConfigRequest(
        @JsonProperty("check_interval_seconds") Integer checkIntervalSeconds,
        @JsonProperty("request_timeout_ms") Integer requestTimeoutMs
) {
}

