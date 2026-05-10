package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookRequest {
    @JsonProperty("url") private String url;
    public String getUrl() { return url; }
}