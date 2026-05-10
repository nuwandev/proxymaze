# ProxyMaze '26

[![Build Status](https://img.shields.io/github/actions/workflow/status/your-org/proxymaze/ci.yml?branch=main)](https://github.com/your-org/proxymaze/actions)
[![Java](https://img.shields.io/badge/Java-21-blue?logo=java)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-brightgreen?logo=spring)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-blue?logo=apachemaven)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-ready-blue?logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Visitors](https://visitor-badge.glitch.me/badge?page_id=your-org/proxymaze)](https://github.com/your-org/proxymaze)
[![GitHub stars](https://img.shields.io/github/stars/your-org/proxymaze?style=social)](https://github.com/your-org/proxymaze)

A real-time proxy pool monitoring and alerting system. Continuously probes a pool of proxy URLs in the background, tracks their health, fires alerts when the failure rate breaches a threshold, and delivers webhook notifications to registered receivers.

Built for the Torch Labs Sri Lanka 2026 Engineering Challenge.

---

## Overview

Traditional proxy monitoring is reactive — someone notices things are broken after clients complain. ProxyMaze flips that. It watches your proxy pool continuously, on its own schedule, and wakes you up before the client emails you.

The system maintains a live health picture of every proxy in the pool. When enough proxies fail to push the pool failure rate past 20%, an alert fires automatically. When the pool recovers, the alert resolves. Every state change is delivered to registered webhook receivers, with retries on transient failures.

---

## Features

- Continuous background monitoring on a configurable schedule
- Real HTTP probes — status comes from actual network results; never mocked.
- Three proxy states: `pending`, `up`, `down`
- Pool failure rate calculation with configurable alert threshold (default 20%)
- Single alert lifecycle per breach — no duplicates, no missed resolutions
- Webhook delivery with automatic retry on transient receiver failures
- Slack and Discord formatted alert payloads
- Full alert history that survives proxy pool resets.
- Consistent state across all endpoints and webhook payloads.

---

## Tech stack

- Java 21
- Spring Boot 3
- Maven
- Java HttpClient (java.net.http.HttpClient)
- In-memory state (ConcurrentHashMap, AtomicReference, AtomicLong)
- ScheduledExecutorService

---

## Getting started

### Prerequisites

- Java 21
- Maven 3.9+

### Run locally

```bash
git clone https://github.com/your-org/proxymaze.git
cd proxymaze
mvn spring-boot:run
```

Service starts on `http://localhost:8080`.

### Run with Docker

```bash
docker build -t proxymaze .
docker run -p 8080:8080 proxymaze
```

---

## API

### Health

| Method | Endpoint  | Description            |
| ------ | --------- | ---------------------- |
| GET    | `/health` | Service liveness check |

```json
{ "status": "ok" }
```

---

### Configuration

| Method | Endpoint  | Description                     |
| ------ | --------- | ------------------------------- |
| POST   | `/config` | Update monitoring configuration |
| GET    | `/config` | Get current configuration       |

```json
{
  "check_interval_seconds": 15,
  "request_timeout_ms": 3000
}
```

Configuration applies immediately to the next monitoring cycle.

---

### Proxy pool

| Method | Endpoint                | Description                           |
| ------ | ----------------------- | ------------------------------------- |
| POST   | `/proxies`              | Add or replace proxies in the pool    |
| GET    | `/proxies`              | Get pool summary and all proxy states |
| GET    | `/proxies/{id}`         | Get detailed state for a single proxy |
| GET    | `/proxies/{id}/history` | Get check history for a single proxy  |
| DELETE | `/proxies`              | Clear the proxy pool                  |

**POST /proxies**

```json
{
  "proxies": [
    "https://provider.example/proxy/px-101",
    "https://provider.example/proxy/px-102"
  ],
  "replace": true
}
```

`replace: true` clears the current pool before loading. `replace: false` or omitted appends to the current pool. New proxies start as `pending` and transition automatically on the next probe cycle.

**GET /proxies**

```json
{
  "total": 10,
  "up": 7,
  "down": 3,
  "failure_rate": 0.3,
  "proxies": [
    {
      "id": "px-101",
      "url": "https://provider.example/proxy/px-101",
      "status": "up",
      "last_checked_at": "2026-04-24T10:15:30Z",
      "consecutive_failures": 0
    }
  ]
}
```

**GET /proxies/{id}**

```json
{
  "id": "px-101",
  "url": "https://provider.example/proxy/px-101",
  "status": "up",
  "last_checked_at": "2026-04-24T10:15:30Z",
  "consecutive_failures": 0,
  "total_checks": 12,
  "uptime_percentage": 91.7,
  "history": [{ "checked_at": "2026-04-24T10:15:30Z", "status": "up" }]
}
```

---

### Alerts

| Method | Endpoint  | Description                         |
| ------ | --------- | ----------------------------------- |
| GET    | `/alerts` | Get all alerts, active and resolved |

```json
[
  {
    "alert_id": "alert-1",
    "status": "active",
    "failure_rate": 0.3,
    "total_proxies": 10,
    "failed_proxies": 3,
    "failed_proxy_ids": ["px-103", "px-104", "px-105"],
    "threshold": 0.2,
    "fired_at": "2026-04-24T10:20:00Z",
    "resolved_at": null,
    "message": "Proxy pool failure rate exceeded threshold"
  }
]
```

---

### Webhooks

| Method | Endpoint    | Description                 |
| ------ | ----------- | --------------------------- |
| POST   | `/webhooks` | Register a webhook receiver |

```json
{ "url": "https://receiver.example/hook" }
```

Response:

```json
{ "webhook_id": "wh-123", "url": "https://receiver.example/hook" }
```

---

### Integrations

| Method | Endpoint        | Description                             |
| ------ | --------------- | --------------------------------------- |
| POST   | `/integrations` | Register a Slack or Discord integration |

**Slack**

```json
{
  "type": "slack",
  "webhook_url": "https://hooks.slack.com/services/...",
  "username": "ProxyWatch",
  "events": ["alert.fired", "alert.resolved"]
}
```

**Discord**

```json
{
  "type": "discord",
  "webhook_url": "https://discord.com/api/webhooks/...",
  "username": "ProxyWatch",
  "events": ["alert.fired", "alert.resolved"]
}
```

---

### Metrics

| Method | Endpoint   | Description             |
| ------ | ---------- | ----------------------- |
| GET    | `/metrics` | Get operational metrics |

```json
{
  "total_checks": 120,
  "current_pool_size": 10,
  "active_alerts": 1,
  "total_alerts": 3,
  "webhook_deliveries": 4
}
```

---

## Webhook payloads

### alert.fired

```json
{
  "event": "alert.fired",
  "alert_id": "alert-1",
  "fired_at": "2026-04-24T10:20:00Z",
  "failure_rate": 0.3,
  "total_proxies": 10,
  "failed_proxies": 3,
  "failed_proxy_ids": ["px-103", "px-104", "px-105"],
  "threshold": 0.2,
  "message": "Proxy pool failure rate exceeded threshold"
}
```

### alert.resolved

```json
{
  "event": "alert.resolved",
  "alert_id": "alert-1",
  "resolved_at": "2026-04-24T10:30:00Z"
}
```

Delivery is attempted to every registered receiver within 60 seconds. Transient failures (500, 502, 503, 504) are retried until success. Each event is delivered exactly once per receiver.

---

## Alert lifecycle

```
Normal ──(rate ≥ 0.20)──► Active ──(rate < 0.20)──► Resolved
                                                         │
                                                 (rate ≥ 0.20)
                                                         │
                                                         ▼
                                                   New alert (new ID)
```

- At most one alert is active at any time
- A sustained breach never creates duplicate alerts or duplicate webhook deliveries
- After resolution, a fresh breach mints a new alert ID
- Alert history is permanent — clearing the proxy pool does not affect it

---

## Project structure

```
src/main/java/com/binarybeasts/
  controller/       HTTP layer — thin, delegates to services
  service/          Business logic interfaces and implementations
  engine/           MonitoringEngine (background probing) and DeliveryEngine (webhook delivery)
  store/            InMemoryStateStore — single canonical state
  domain/           ProxyNode, Alert, RuntimeConfig, enums
  dto/              Request and response objects
  integration/      SlackFormatter and DiscordFormatter
  util/             ProxyIdExtractor, LogHighlighter
```

---

## Behavioral guarantees

- Monitoring runs on its own schedule — read endpoints never trigger probes
- Proxy IDs are deterministic — final path segment of the URL
- All timestamps are ISO 8601 UTC
- Unknown fields in request bodies are silently ignored.
- State is consistent across `/proxies`, `/alerts`, and webhook payloads at all times

---

## License

MIT — see [LICENSE](LICENSE).
