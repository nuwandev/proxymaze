package com.binarybeasts.domain;

import java.time.Instant;

public class ProxyCheckRecord {
    private final Instant checkedAt;
    private final ProxyStatus status;

    public ProxyCheckRecord(Instant checkedAt, ProxyStatus status) {
        this.checkedAt = checkedAt;
        this.status = status;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public ProxyStatus getStatus() {
        return status;
    }
}