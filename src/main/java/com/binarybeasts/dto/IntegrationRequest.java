package com.binarybeasts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IntegrationRequest {
    @JsonProperty("type") private String type;
    @JsonProperty("webhook_url") private String webhookUrl;
    @JsonProperty("username") private String username;
    @JsonProperty("events") private List<String> events;
    public String getType() { return type; }
    public String getWebhookUrl() { return webhookUrl; }
}