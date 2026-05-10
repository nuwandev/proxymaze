package com.binarybeasts.engine;

import com.binarybeasts.domain.*;
import com.binarybeasts.integration.DiscordFormatter;
import com.binarybeasts.integration.SlackFormatter;
import com.binarybeasts.service.WebhookService;
import com.binarybeasts.store.InMemoryStateStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Component
public class DeliveryEngine implements WebhookService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryEngine.class);

    @Autowired private InMemoryStateStore store;

    private final BlockingQueue<DeliveryJob> queue = new LinkedBlockingQueue<>();
    private final Set<String> delivered = ConcurrentHashMap.newKeySet();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void start() {
        Thread worker = new Thread(this::processQueue, "delivery-worker");
        worker.setDaemon(true);
        worker.start();
        log.info("[Delivery] Engine started");
    }

    @Override
    public void enqueue(Alert alert, String eventType) {
        // Queue for registered webhooks
        store.getWebhooks().forEach(wh -> {
            String key = alert.getAlertId() + ":" + eventType + ":" + wh.webhookId();
            if (!delivered.contains(key)) {
                queue.offer(new DeliveryJob(alert, eventType, wh.url(), key, "webhook"));
            }
        });

        // Queue for integrations
        store.getIntegrations().forEach(integration -> {
            if (integration.getEvents().contains(eventType)) {
                String key = alert.getAlertId() + ":" + eventType + ":" + integration.getId();
                if (!delivered.contains(key)) {
                    queue.offer(new DeliveryJob(alert, eventType,
                            integration.getWebhookUrl(), key, integration.getType(),
                            integration.getUsername()));
                }
            }
        });
    }

    private void processQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                DeliveryJob job = queue.take();
                if (!delivered.contains(job.key())) {
                    deliver(job);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void deliver(DeliveryJob job) {
        int attempt = 0;
        while (true) {
            try {
                Map<String, Object> payload = buildPayload(job);
                String body = mapper.writeValueAsString(payload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(job.url()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .timeout(Duration.ofSeconds(10))
                        .build();

                HttpResponse<String> response = http.send(request,
                        HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();

                if (status >= 200 && status < 300) {
                    delivered.add(job.key());
                    store.incrementWebhookDeliveries();
                    log.info("[Delivery] Success — {} to {}", job.eventType(), job.url());
                    return;
                } else if (status == 500 || status == 502 || status == 503 || status == 504) {
                    attempt++;
                    log.warn("[Delivery] Retryable failure {} (attempt {}) — {}",
                            status, attempt, job.url());
                    Thread.sleep(Math.min(1000L * attempt, 10000L)); // backoff
                } else {
                    log.warn("[Delivery] Non-retryable failure {} — {}", status, job.url());
                    return; // give up
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                attempt++;
                log.warn("[Delivery] Error attempt {} — {}: {}", attempt, job.url(), e.getMessage());
                try { Thread.sleep(Math.min(1000L * attempt, 10000L)); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    private Map<String, Object> buildPayload(DeliveryJob job) {
        Alert alert = job.alert();
        String eventType = job.eventType();
        String type = job.integrationType();
        String username = job.username();

        if ("slack".equals(type)) {
            return eventType.equals("alert.fired")
                    ? SlackFormatter.formatFired(alert, username)
                    : SlackFormatter.formatResolved(alert, username);
        } else if ("discord".equals(type)) {
            return eventType.equals("alert.fired")
                    ? DiscordFormatter.formatFired(alert, username)
                    : DiscordFormatter.formatResolved(alert, username);
        } else {
            // Plain webhook payload
            if (eventType.equals("alert.fired")) {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("event", "alert.fired");
                p.put("alert_id", alert.getAlertId());
                p.put("fired_at", alert.getFiredAt().toString());
                p.put("failure_rate", alert.getFailureRate());
                p.put("total_proxies", alert.getTotalProxies());
                p.put("failed_proxies", alert.getFailedProxies());
                p.put("failed_proxy_ids", alert.getFailedProxyIds());
                p.put("threshold", alert.getThreshold());
                p.put("message", alert.getMessage());
                return p;
            } else {
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("event", "alert.resolved");
                p.put("alert_id", alert.getAlertId());
                p.put("resolved_at", alert.getResolvedAt() != null
                        ? alert.getResolvedAt().toString() : null);
                return p;
            }
        }
    }
}
