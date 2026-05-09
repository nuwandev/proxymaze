package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ProxyCheckRecordResponse(
        @JsonProperty("checked_at") Instant checkedAt,
        @JsonProperty("status") String status
) {
}

