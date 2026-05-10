package com.binarybeasts.controller;

import com.binarybeasts.domain.*;
import com.binarybeasts.dto.AddProxiesRequest;
import com.binarybeasts.dto.ConfigRequest;
import com.binarybeasts.dto.IntegrationRequest;
import com.binarybeasts.dto.WebhookRequest;
import com.binarybeasts.service.AlertService;
import com.binarybeasts.service.MetricsService;
import com.binarybeasts.service.MonitoringService;
import com.binarybeasts.service.WebhookService;
import com.binarybeasts.store.InMemoryStateStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class MonitoringController {

    @Autowired
    private MonitoringService monitoringService;
    @Autowired
    private AlertService alertService;
    @Autowired
    private MetricsService metricsService;
    @Autowired
    private InMemoryStateStore store;
    @Autowired(required = false)
    private WebhookService webhookService;

    // ─── GET /health ────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // ─── POST /config ────────────────────────────────────────────────────────

    @PostMapping("/config")
    public ResponseEntity<?> updateConfig(@RequestBody ConfigRequest req) {
        monitoringService.updateConfig(req.getCheckIntervalSeconds(), req.getRequestTimeoutMs());
        return ResponseEntity.ok(Map.of(
                "check_interval_seconds", req.getCheckIntervalSeconds(),
                "request_timeout_ms", req.getRequestTimeoutMs()
        ));
    }

    // ─── GET /config ─────────────────────────────────────────────────────────

    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        RuntimeConfig cfg = monitoringService.getConfig();
        return ResponseEntity.ok(Map.of(
                "check_interval_seconds", cfg.getCheckIntervalSeconds(),
                "request_timeout_ms", cfg.getRequestTimeoutMs()
        ));
    }

    // ─── POST /proxies ────────────────────────────────────────────────────────

    @PostMapping("/proxies")
    public ResponseEntity<?> ingestProxies(@RequestBody AddProxiesRequest req) {
        List<ProxyNode> added = monitoringService.addProxies(req.getProxies(), req.isReplace());

        List<Map<String, Object>> proxyList = added.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("url", p.getUrl());
            m.put("status", p.getStatus().name().toLowerCase());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.status(201).body(Map.of(
                "accepted", added.size(),
                "proxies", proxyList
        ));
    }

    // ─── GET /proxies ─────────────────────────────────────────────────────────

    @GetMapping("/proxies")
    public ResponseEntity<?> getProxies() {
        Collection<ProxyNode> all = monitoringService.getAllProxies();

        long up = all.stream().filter(p -> p.getStatus() == ProxyStatus.UP).count();
        long down = all.stream().filter(p -> p.getStatus() == ProxyStatus.DOWN).count();
        double failureRate = monitoringService.calculateFailureRate();

        List<Map<String, Object>> proxyList = all.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("url", p.getUrl());
            m.put("status", p.getStatus().name().toLowerCase());
            m.put("last_checked_at", p.getLastCheckedAt() != null
                    ? p.getLastCheckedAt().toString() : null);
            m.put("consecutive_failures", p.getConsecutiveFailures());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", all.size());
        response.put("up", up);
        response.put("down", down);
        response.put("failure_rate", failureRate);
        response.put("proxies", proxyList);

        return ResponseEntity.ok(response);
    }

    // ─── GET /proxies/{id} ────────────────────────────────────────────────────

    @GetMapping("/proxies/{id}")
    public ResponseEntity<?> getProxy(@PathVariable String id) {
        return monitoringService.getProxy(id)
                .map(p -> {
                    List<Map<String, Object>> history = p.getHistory().stream().map(r -> {
                        Map<String, Object> h = new LinkedHashMap<>();
                        h.put("checked_at", r.checkedAt().toString());
                        h.put("status", r.status().name().toLowerCase());
                        return h;
                    }).collect(Collectors.toList());

                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("url", p.getUrl());
                    m.put("status", p.getStatus().name().toLowerCase());
                    m.put("last_checked_at", p.getLastCheckedAt() != null
                            ? p.getLastCheckedAt().toString() : null);
                    m.put("consecutive_failures", p.getConsecutiveFailures());
                    m.put("total_checks", p.getTotalChecks());
                    m.put("uptime_percentage", p.getUptimePercentage());
                    m.put("history", history);

                    return ResponseEntity.ok(m);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── GET /proxies/{id}/history ────────────────────────────────────────────

    @GetMapping("/proxies/{id}/history")
    public ResponseEntity<?> getProxyHistory(@PathVariable String id) {
        return monitoringService.getProxy(id)
                .map(p -> {
                    List<Map<String, Object>> history = p.getHistory().stream().map(r -> {
                        Map<String, Object> h = new LinkedHashMap<>();
                        h.put("checked_at", r.checkedAt().toString());
                        h.put("status", r.status().name().toLowerCase());
                        return h;
                    }).collect(Collectors.toList());
                    return ResponseEntity.ok(history);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── DELETE /proxies ──────────────────────────────────────────────────────

    @DeleteMapping("/proxies")
    public ResponseEntity<?> clearProxies() {
        monitoringService.clearPool();
        return ResponseEntity.noContent().build();
    }

    // ─── GET /alerts ──────────────────────────────────────────────────────────

    @GetMapping("/alerts")
    public ResponseEntity<?> getAlerts() {
        List<Map<String, Object>> alerts = alertService.getAllAlerts().stream()
                .map(this::toAlertMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(alerts);
    }

    private Map<String, Object> toAlertMap(Alert alert) {
        // Active alert → live failed proxy IDs
        // Resolved alert → frozen snapshot
        List<String> failedIds;
        if (alert.getStatus() == AlertStatus.ACTIVE) {
            failedIds = store.getAllProxies().stream()
                    .filter(p -> p.getStatus() == ProxyStatus.DOWN)
                    .map(ProxyNode::getId)
                    .collect(Collectors.toList());
        } else {
            failedIds = alert.getFailedProxyIds();
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("alert_id", alert.getAlertId());
        m.put("status", alert.getStatus().name().toLowerCase());
        m.put("failure_rate", alert.getFailureRate());
        m.put("total_proxies", alert.getTotalProxies());
        m.put("failed_proxies", alert.getFailedProxies());
        m.put("failed_proxy_ids", failedIds);
        m.put("threshold", alert.getThreshold());
        m.put("fired_at", alert.getFiredAt().toString());
        m.put("resolved_at", alert.getResolvedAt() != null
                ? alert.getResolvedAt().toString() : null);
        m.put("message", alert.getMessage());
        return m;
    }

    // ─── POST /webhooks ───────────────────────────────────────────────────────

    @PostMapping("/webhooks")
    public ResponseEntity<?> registerWebhook(@RequestBody WebhookRequest req) {
        String id = "wh-" + System.currentTimeMillis();
        WebhookRegistration reg = new WebhookRegistration(id, req.getUrl());
        store.addWebhook(reg);
        return ResponseEntity.status(201).body(Map.of(
                "webhook_id", id,
                "url", req.getUrl()
        ));
    }

    // ─── POST /integrations ───────────────────────────────────────────────────

    @PostMapping("/integrations")
    public ResponseEntity<?> registerIntegration(@RequestBody IntegrationRequest req) {
        String id = "int-" + System.currentTimeMillis();
        return ResponseEntity.status(201).body(Map.of(
                "id", id,
                "type", req.getType(),
                "status", "accepted"
        ));
    }

    // ─── GET /metrics ─────────────────────────────────────────────────────────

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        return ResponseEntity.ok(metricsService.getMetrics());
    }
}