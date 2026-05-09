package com.binarybeasts.controller;

import com.binarybeasts.domain.Alert;
import com.binarybeasts.domain.AlertStatus;
import com.binarybeasts.domain.ProxyCheckRecord;
import com.binarybeasts.domain.ProxyNode;
import com.binarybeasts.domain.ProxyStatus;
import com.binarybeasts.domain.RuntimeConfig;
import com.binarybeasts.dto.AlertResponse;
import com.binarybeasts.dto.HealthResponse;
import com.binarybeasts.dto.MetricsResponse;
import com.binarybeasts.dto.ProxyCheckRecordResponse;
import com.binarybeasts.dto.ProxyDetailsResponse;
import com.binarybeasts.dto.ProxyIngestionResponse;
import com.binarybeasts.dto.ProxySummaryResponse;
import com.binarybeasts.dto.ProxiesCollectionResponse;
import com.binarybeasts.dto.ProxiesRequest;
import com.binarybeasts.dto.RuntimeConfigRequest;
import com.binarybeasts.dto.RuntimeConfigResponse;
import com.binarybeasts.service.AlertService;
import com.binarybeasts.service.MetricsService;
import com.binarybeasts.service.MetricsSnapshot;
import com.binarybeasts.service.MonitoringService;
import com.binarybeasts.store.InMemoryStateStore;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
public class MonitoringController {
    private final InMemoryStateStore store;
    private final MonitoringService monitoringService;
    private final AlertService alertService;
    private final MetricsService metricsService;

    public MonitoringController(InMemoryStateStore store,
                                MonitoringService monitoringService,
                                AlertService alertService,
                                MetricsService metricsService) {
        this.store = store;
        this.monitoringService = monitoringService;
        this.alertService = alertService;
        this.metricsService = metricsService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok");
    }

    @PostMapping("/config")
    public RuntimeConfigResponse updateConfig(@RequestBody RuntimeConfigRequest request) {
        requireConfig(request);
        store.updateRuntimeConfig(request.checkIntervalSeconds(), request.requestTimeoutMs());
        return toRuntimeConfigResponse(store.getRuntimeConfig());
    }

    @GetMapping("/config")
    public RuntimeConfigResponse getConfig() {
        return toRuntimeConfigResponse(store.getRuntimeConfig());
    }

    @PostMapping("/proxies")
    public ResponseEntity<ProxyIngestionResponse> ingestProxies(@RequestBody ProxiesRequest request) {
        if (request == null || request.proxies() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "proxies is required");
        }

        List<ProxyNode> proxies = Boolean.TRUE.equals(request.replace())
                ? store.replaceProxies(request.proxies())
                : store.addProxies(request.proxies());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ProxyIngestionResponse(true, proxies.stream().map(this::toSummaryResponse).toList()));
    }

    @GetMapping("/proxies")
    public ProxiesCollectionResponse getProxies() {
        List<ProxyNode> proxies = monitoringService.snapshotProxies();
        return new ProxiesCollectionResponse(
                proxies.size(),
                store.getUpCount(),
                store.getDownCount(),
                store.getFailureRate(),
                proxies.stream().map(this::toSummaryResponse).toList()
        );
    }

    @GetMapping("/proxies/{id}")
    public ProxyDetailsResponse getProxy(@PathVariable String id) {
        ProxyNode proxy = monitoringService.findProxy(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proxy not found"));
        return toDetailsResponse(proxy);
    }

    @GetMapping("/proxies/{id}/history")
    public List<ProxyCheckRecordResponse> getProxyHistory(@PathVariable String id) {
        ProxyNode proxy = monitoringService.findProxy(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proxy not found"));
        return proxy.getHistory().stream().map(this::toHistoryResponse).toList();
    }

    @DeleteMapping("/proxies")
    public ResponseEntity<Void> clearProxies() {
        store.clearProxies();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/alerts")
    public List<AlertResponse> getAlerts() {
        return alertService.getAlerts().stream().map(this::toAlertResponse).toList();
    }

    @GetMapping("/metrics")
    public MetricsResponse getMetrics() {
        MetricsSnapshot snapshot = metricsService.snapshot();
        return new MetricsResponse(
                snapshot.totalChecks(),
                snapshot.currentPoolSize(),
                snapshot.activeAlerts(),
                snapshot.totalAlerts(),
                snapshot.webhookDeliveries()
        );
    }

    private void requireConfig(RuntimeConfigRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "config body is required");
        }
        if (request.checkIntervalSeconds() == null || request.requestTimeoutMs() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "check_interval_seconds and request_timeout_ms are required");
        }
        if (request.checkIntervalSeconds() <= 0 || request.requestTimeoutMs() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "config values must be positive");
        }
    }

    private RuntimeConfigResponse toRuntimeConfigResponse(RuntimeConfig config) {
        return new RuntimeConfigResponse(config.getCheckIntervalSeconds(), config.getRequestTimeoutMs());
    }

    private ProxySummaryResponse toSummaryResponse(ProxyNode proxy) {
        return new ProxySummaryResponse(
                proxy.getId(),
                proxy.getUrl(),
                statusValue(proxy.getStatus()),
                proxy.getLastCheckedAt(),
                proxy.getConsecutiveFailures()
        );
    }

    private ProxyDetailsResponse toDetailsResponse(ProxyNode proxy) {
        return new ProxyDetailsResponse(
                proxy.getId(),
                proxy.getUrl(),
                statusValue(proxy.getStatus()),
                proxy.getLastCheckedAt(),
                proxy.getConsecutiveFailures(),
                proxy.getTotalChecks(),
                proxy.getUptimePercentage(),
                proxy.getHistory().stream().map(this::toHistoryResponse).toList()
        );
    }

    private ProxyCheckRecordResponse toHistoryResponse(ProxyCheckRecord record) {
        return new ProxyCheckRecordResponse(record.getCheckedAt(), statusValue(record.getStatus()));
    }

    private AlertResponse toAlertResponse(Alert alert) {
        return new AlertResponse(
                alert.getAlertId(),
                statusValue(alert.getStatus()),
                alert.getFailureRate(),
                alert.getTotalProxies(),
                alert.getFailedProxies(),
                alert.getFailedProxyIds(),
                alert.getThreshold(),
                alert.getFiredAt(),
                alert.getResolvedAt(),
                alert.getMessage()
        );
    }

    private String statusValue(ProxyStatus status) {
        return status.name().toLowerCase();
    }

    private String statusValue(AlertStatus status) {
        return status.name().toLowerCase();
    }
}

