package com.binarybeasts.engine;

import com.binarybeasts.domain.ProxyStatus;

public record ProbeOutcome(ProxyStatus status, int httpStatusCode, String errorMessage) {
    public static ProbeOutcome up(int httpStatusCode) {
        return new ProbeOutcome(ProxyStatus.UP, httpStatusCode, null);
    }

    public static ProbeOutcome down(String errorMessage) {
        return new ProbeOutcome(ProxyStatus.DOWN, -1, errorMessage);
    }
}

