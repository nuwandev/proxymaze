package com.binarybeasts.service;

import com.binarybeasts.domain.Alert;

public interface WebhookService {
    void enqueue(Alert alert, String s);
}
