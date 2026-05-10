package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProxiesRequest(
        @JsonProperty("proxies") List<String> proxies,
        @JsonProperty("replace") Boolean replace
) {
}

