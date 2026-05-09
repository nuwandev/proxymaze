package com.binarybeasts.engine;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class HttpClientProxyProbeClient implements ProxyProbeClient {
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public ProbeOutcome probe(String url, int timeoutMs) {
        int safeTimeoutMs = Math.max(1, timeoutMs);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(safeTimeoutMs))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return ProbeOutcome.up(statusCode);
            }
            return ProbeOutcome.down("HTTP " + statusCode);
        } catch (Exception ex) {
            return ProbeOutcome.down(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }
}

