package com.binarybeasts.integration;

import com.binarybeasts.domain.Alert;
import com.binarybeasts.domain.AlertStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class SlackFormatter {

    public static Map<String, Object> formatFired(Alert alert, String username) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username != null ? username : "ProxyWatch");
        payload.put("text", "Alert Fired — Proxy pool failure rate exceeded threshold");

        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("color", "#FF0000");
        attachment.put("fields", List.of(
                field("Alert ID", alert.getAlertId()),
                field("Failure Rate", String.format("%.2f%%", alert.getFailureRate() * 100)),
                field("Failed Proxies", alert.getFailedProxies() + " / " + alert.getTotalProxies()),
                field("Threshold", "20%"),
                field("Failed IDs", String.join(", ", alert.getFailedProxyIds())),
                field("Fired At", alert.getFiredAt().toString())
        ));
        attachment.put("footer", "ProxyMaze'26 — Torch Labs");
        attachment.put("ts", alert.getFiredAt().getEpochSecond()); // integer unix timestamp

        payload.put("attachments", List.of(attachment));
        return payload;
    }

    public static Map<String, Object> formatResolved(Alert alert, String username) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username != null ? username : "ProxyWatch");
        payload.put("text", "Alert Resolved — Proxy pool has recovered");

        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("color", "#36A64F");
        attachment.put("fields", List.of(
                field("Alert ID", alert.getAlertId()),
                field("Failure Rate", String.format("%.2f%%", alert.getFailureRate() * 100)),
                field("Failed Proxies", alert.getFailedProxies() + " / " + alert.getTotalProxies()),
                field("Threshold", "20%"),
                field("Failed IDs", String.join(", ", alert.getFailedProxyIds())),
                field("Fired At", alert.getFiredAt().toString())
        ));
        attachment.put("footer", "ProxyMaze'26 — Torch Labs");
        attachment.put("ts", alert.getResolvedAt() != null
                ? alert.getResolvedAt().getEpochSecond()
                : Instant.now().getEpochSecond());

        payload.put("attachments", List.of(attachment));
        return payload;
    }

    private static Map<String, Object> field(String title, String value) {
        return Map.of("title", title, "value", value);
    }
}
