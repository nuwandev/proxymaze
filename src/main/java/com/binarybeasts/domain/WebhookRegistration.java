package com.binarybeasts.domain;

public class WebhookRegistration {
    private final String webhookId;
    private final String url;

    public WebhookRegistration(String webhookId, String url) {
        this.webhookId = webhookId;
        this.url = url;
    }

    public String getWebhookId() {
        return webhookId;
    }

    public String getUrl() {
        return url;
    }
}