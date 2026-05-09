package com.binarybeasts.service;

import com.binarybeasts.domain.Alert;

public record AlertLifecycleOutcome(boolean fired, boolean resolved, Alert alert) {
}

