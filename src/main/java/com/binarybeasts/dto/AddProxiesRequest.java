package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AddProxiesRequest {
    @JsonProperty("proxies")
    private List<String> proxies = new ArrayList<>();
    @JsonProperty("replace")
    private boolean replace = false;

    public List<String> getProxies() {
        return proxies;
    }

    public boolean isReplace() {
        return replace;
    }
}