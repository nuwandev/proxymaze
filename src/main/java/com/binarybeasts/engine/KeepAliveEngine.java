package com.binarybeasts.engine;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class KeepAliveEngine {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveEngine.class);

    @Value("${app.base-url:}")
    private String baseUrl;

    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @PostConstruct
    public void start() {
        if (baseUrl == null || baseUrl.isBlank()) {
            log.info("[KeepAlive] No base URL configured, skipping");
            return;
        }

        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "keep-alive");
            t.setDaemon(false);
            return t;
        }).scheduleWithFixedDelay(this::ping, 4, 4, TimeUnit.MINUTES);

        log.info("[KeepAlive] Started — pinging {} every 4 minutes", baseUrl);
    }

    private void ping() {
        try {
            var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/health"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
            log.debug("[KeepAlive] Ping sent");
        } catch (Exception e) {
            log.warn("[KeepAlive] Ping failed: {}", e.getMessage());
        }
    }
}