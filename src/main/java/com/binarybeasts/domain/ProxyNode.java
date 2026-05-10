package com.binarybeasts.domain;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ProxyNode {
    private final String id;
    private final String url;

    private volatile ProxyStatus status = ProxyStatus.PENDING;
    private volatile Instant lastCheckedAt = null;
    private volatile int consecutiveFailures = 0;
    private volatile long totalChecks = 0;
    private volatile long successfulChecks = 0;

    private final Deque<ProxyCheckRecord> history = new ArrayDeque<>();

    public ProxyNode(String id, String url) {
        this.id = id;
        this.url = url;
    }

    public synchronized void recordSuccess(Instant now) {
        this.status = ProxyStatus.UP;
        this.lastCheckedAt = now;
        this.consecutiveFailures = 0;
        this.totalChecks++;
        this.successfulChecks++;
        addHistory(new ProxyCheckRecord(now, ProxyStatus.UP));
    }

    public synchronized void recordFailure(Instant now) {
        this.status = ProxyStatus.DOWN;
        this.lastCheckedAt = now;
        this.consecutiveFailures++;
        this.totalChecks++;
        addHistory(new ProxyCheckRecord(now, ProxyStatus.DOWN));
    }

    private void addHistory(ProxyCheckRecord record) {
        if (history.size() >= 100) history.pollFirst();
        history.addLast(record);
    }

    public synchronized List<ProxyCheckRecord> getHistory() {
        return new ArrayList<>(history);
    }

    public double getUptimePercentage() {
        if (totalChecks == 0) return 0.0;
        return (double) successfulChecks / totalChecks * 100.0;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public ProxyStatus getStatus() {
        return status;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public long getTotalChecks() {
        return totalChecks;
    }

    public long getSuccessfulChecks() {
        return successfulChecks;
    }
}