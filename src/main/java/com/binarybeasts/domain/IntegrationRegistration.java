package com.binarybeasts.domain;

import java.util.List;

public class IntegrationRegistration {
    private final String id;
    private final String type; // "slack" or "discord"
    private final String webhookUrl;
    private final String username;
    private final List<String> events; // ["alert.fired", "alert.resolved"]

    public IntegrationRegistration(String id, String type, String webhookUrl,
                                   String username, List<String> events) {
        this.id = id;
        this.type = type;
        this.webhookUrl = webhookUrl;
        this.username = username;
        this.events = List.copyOf(events);
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getWebhookUrl() { return webhookUrl; }
    public String getUsername() { return username; }
    public List<String> getEvents() { return events; }
}