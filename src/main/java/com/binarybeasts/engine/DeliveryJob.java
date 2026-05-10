package com.binarybeasts.engine;

import com.binarybeasts.domain.Alert;

public record DeliveryJob(
        Alert alert,
        String eventType,
        String url,
        String key,
        String integrationType,
        String username
) {
    // Constructor for plain webhooks (no username)
    public DeliveryJob(Alert alert, String eventType, String url,
                       String key, String integrationType) {
        this(alert, eventType, url, key, integrationType, null);
    }
}