package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
        (ignoreUnknown = true)
public class ConfigRequest {
    @JsonProperty("check_interval_seconds")
    private int checkIntervalSeconds;
    @JsonProperty("request_timeout_ms")
    private int requestTimeoutMs;

    public int getCheckIntervalSeconds() {
        return checkIntervalSeconds;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }
}
