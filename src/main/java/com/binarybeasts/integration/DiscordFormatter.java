package com.binarybeasts.integration;

import com.binarybeasts.domain.Alert;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class DiscordFormatter {

    public static Map<String, Object> formatFired(Alert alert, String username) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "Alert Fired");
        embed.put("description", "Proxy pool failure rate has exceeded the threshold.");
        embed.put("color", 16711680); // red — integer 0-16777215
        embed.put("fields", List.of(
                field("Alert ID", alert.getAlertId()),
                field("Failure Rate", String.format("%.2f%%", alert.getFailureRate() * 100)),
                field("Failed Proxies", alert.getFailedProxies() + " / " + alert.getTotalProxies()),
                field("Threshold", "20%"),
                field("Failed IDs", String.join(", ", alert.getFailedProxyIds()))
        ));
        embed.put("footer", Map.of("text", "ProxyMaze'26 — Torch Labs"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username != null ? username : "ProxyWatch");
        payload.put("embeds", List.of(embed));
        return payload;
    }

    public static Map<String, Object> formatResolved(Alert alert, String username) {
        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "Alert Resolved");
        embed.put("description", "Proxy pool has recovered below the failure threshold.");
        embed.put("color", 3580392); // green
        embed.put("fields", List.of(
                field("Alert ID", alert.getAlertId()),
                field("Failure Rate", String.format("%.2f%%", alert.getFailureRate() * 100)),
                field("Failed Proxies", alert.getFailedProxies() + " / " + alert.getTotalProxies()),
                field("Threshold", "20%"),
                field("Failed IDs", String.join(", ", alert.getFailedProxyIds()))
        ));
        embed.put("footer", Map.of("text", "ProxyMaze'26 — Torch Labs"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username != null ? username : "ProxyWatch");
        payload.put("embeds", List.of(embed));
        return payload;
    }

    private static Map<String, Object> field(String name, String value) {
        return Map.of("name", name, "value", value);
    }
}
