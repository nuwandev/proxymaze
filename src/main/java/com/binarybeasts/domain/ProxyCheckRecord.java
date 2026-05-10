package com.binarybeasts.domain;

import java.time.Instant;

public record ProxyCheckRecord(Instant checkedAt, ProxyStatus status) {
}