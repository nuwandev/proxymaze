package com.binarybeasts.domain;

import java.time.Instant;
import java.util.List;

public class Alert {
    private final String alertId;
    private volatile AlertStatus status;
    private final double failureRate; // frozen at fire
    private final int totalProxies; // frozen at fire
    private final int failedProxies; // frozen at fire
    private final List<String> failedProxyIds; // frozen at fire
    private final double threshold = 0.20;
    private final Instant firedAt;
    private volatile Instant resolvedAt = null;
    private final String message;

    public Alert(String alertId, double failureRate, int totalProxies,
                 int failedProxies, List<String> failedProxyIds, Instant firedAt) {
        this.alertId = alertId;
        this.status = AlertStatus.ACTIVE;
        this.failureRate = failureRate;
        this.totalProxies = totalProxies;
        this.failedProxies = failedProxies;
        this.failedProxyIds = List.copyOf(failedProxyIds);
        this.firedAt = firedAt;
        this.message = String.format("Failure rate %.2f%% exceeds threshold %.0f%%",
                failureRate * 100, threshold * 100);
    }

    public void resolve(Instant now) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = now;
    }

    public String getAlertId() {
        return alertId;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public double getFailureRate() {
        return failureRate;
    }

    public int getTotalProxies() {
        return totalProxies;
    }

    public int getFailedProxies() {
        return failedProxies;
    }

    public List<String> getFailedProxyIds() {
        return failedProxyIds;
    }

    public double getThreshold() {
        return threshold;
    }

    public Instant getFiredAt() {
        return firedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getMessage() {
        return message;
    }
}