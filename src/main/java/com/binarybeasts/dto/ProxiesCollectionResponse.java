package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProxiesCollectionResponse(
        @JsonProperty("total") int total,
        @JsonProperty("up") int up,
        @JsonProperty("down") int down,
        @JsonProperty("failure_rate") double failureRate,
        @JsonProperty("proxies") List<ProxySummaryResponse> proxies
) {
}

