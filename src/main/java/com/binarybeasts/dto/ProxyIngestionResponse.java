package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ProxyIngestionResponse(
        @JsonProperty("accepted") int accepted,
        @JsonProperty("proxies") List<ProxySummaryResponse> proxies
) {
}

