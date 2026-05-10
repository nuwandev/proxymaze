package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HealthResponse(@JsonProperty("status") String status) {
}

