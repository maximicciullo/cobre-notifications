# Cobre — Sr. Software Engineer Case: Notifications

Solution for the event notifications challenge (webhook delivery + self-service API).
Original prompt in `challenge/`.

## Documentation index

| File | What it contains |
|---|---|
| [`NOTES.md`](./NOTES.md) | Extracted checklist of the challenge requirements (from the original PDF) |
| [`DESIGN.md`](./DESIGN.md) | **Task 1** — Solution design: C4 diagrams, delivery/retry sequence, data model, architecture decisions |
| [`SECURITY.md`](./SECURITY.md) | **Task 3** — 3 chosen OWASP Top 10 vulnerabilities (Broken Access Control, Injection, SSRF) and their mitigations, implemented in code |
| [`AI_USAGE.md`](./AI_USAGE.md) | AI usage history during development (prompts, tool, how the output was used) — kept in Spanish, matching the actual prompts used |

## Stack

- **Language/Framework**: Java 21 + Spring Boot 3.5.3 (hexagonal architecture) — plain Java
  and Spring only, no Lombok/H2/WireMock; the one deliberate non-Spring dependency is
  springdoc-openapi for the Swagger docs below
- **Database**: PostgreSQL 16
- **Delivery**: outbox pattern + scheduled poller, retry with exponential backoff
- **API docs**: OpenAPI 3 / Swagger UI (springdoc-openapi), generated from the code
- **Observability**: Micrometer business metrics + Prometheus + Grafana (pre-provisioned dashboard)
- **Containerization**: Docker + Docker Compose (app + Postgres + Prometheus + Grafana)

## How to run

### Prerequisites

- **Docker + Docker Compose** (Docker Desktop on macOS/Windows already includes both;
  `docker compose version` should print something). This is the only hard requirement —
  the app, Postgres, and all dependencies run in containers, so **no local Java or Maven
  install is needed** to run it.
- **Java 21** — only needed if you want to run the test suite (`./mvnw test`) or the app
  directly on the host instead of via Docker. The included Maven wrapper (`./mvnw`) downloads
  Maven itself, so a separate Maven install is never required either way.
- Ports **8082** (API), **5432** (Postgres), **9090** (Prometheus), and **3000** (Grafana) free
  on the host. Change the left-hand side of the relevant `ports:` mapping in
  `docker-compose.yml` if any is already taken.

### 1. Clone and run

```bash
git clone https://github.com/maximicciullo/cobre-notifications.git
cd cobre-notifications/notification-service   # the Spring Boot project lives in this subfolder
docker compose up --build
```

Wait for `Started NotificationServiceApplication in ... seconds` in the logs (~10s on a cold
build) before hitting the API — the container reports "healthy" once Postgres is ready, but
the app itself needs a few more seconds to finish Flyway + JPA startup.

This starts Postgres, runs the Flyway migrations, and seeds the app with the 10 events from
`challenge/notification_events.json` plus 3 demo subscriptions (one per client in that file).
The API is then available at `http://localhost:8082`.

```bash
# Sanity check
curl http://localhost:8082/actuator/health   # -> {"status":"UP"}
```

> **Note:** the Postgres data lives in a named Docker volume, so it **persists across
> `docker compose down` / `up` cycles** (e.g. a replay you triggered earlier will still show
> as `pending`/whatever you left it as). Run `docker compose down -v` instead to wipe the
> volume and start from the original seed data again.

### 2. Call the API

Each seeded client has a fixed API key, sent via the `X-Api-Key` header:

| Client | API key |
|---|---|
| CLIENT001 | `demo-api-key-client001` |
| CLIENT002 | `demo-api-key-client002` |
| CLIENT003 | `demo-api-key-client003` |

```bash
# List CLIENT001's events, optionally filtered
curl -H "X-Api-Key: demo-api-key-client001" \
  "http://localhost:8082/notification_events?delivery_status=failed"

# Get one event's detail
curl -H "X-Api-Key: demo-api-key-client002" \
  "http://localhost:8082/notification_events/EVT003"

# Replay a definitively-failed delivery
curl -X POST -H "X-Api-Key: demo-api-key-client002" \
  "http://localhost:8082/notification_events/EVT003/replay"
```

### 3. Explore the API docs (Swagger UI)

Interactive docs for the 3 endpoints, generated from the code (springdoc-openapi) so they can
never drift from the actual implementation:

- **Swagger UI**: http://localhost:8082/swagger-ui/index.html
- **Raw OpenAPI spec**: http://localhost:8082/v3/api-docs

Click **Authorize** (top right) and paste one of the demo API keys above to try the endpoints
directly from the browser — Swagger UI sends it as the `X-Api-Key` header on every request.

### 4. Watch it in near real-time (Prometheus + Grafana)

`docker compose up` also starts Prometheus (scraping the app every 5s) and Grafana with a
dashboard already provisioned — nothing to configure by hand.

- **Grafana dashboard**: http://localhost:3000/d/cobre-notifications (anonymous viewer access
  enabled; login isn't required — `admin`/`admin` also works if prompted)
- **Prometheus** (raw metrics/targets): http://localhost:9090

The dashboard has 5 panels, all backed by custom Micrometer metrics emitted from
`DeliveryProcessingService` / `ReplayService` / `DeliveryBacklogMetrics` (not just generic
JVM/HTTP metrics):

| Panel | Metric | What it answers |
|---|---|---|
| Delivery outcomes (per minute) | `cobre_delivery_attempts_total{outcome}` | success vs. retry-scheduled vs. dead-letter rate |
| Pending delivery backlog | `cobre_delivery_backlog` | how many deliveries are waiting right now |
| Replays triggered | `cobre_delivery_replays_total` | self-service replay usage |
| Self-service API request rate | `http_server_requests_seconds_count` | traffic on the 3 endpoints |
| Dead-lettered deliveries | `cobre_delivery_attempts_total{outcome="dead_letter"}` | the number a monitoring team would actually alert on |

To see it move live: replay a failed event (step 2 above) and watch the "Delivery outcomes"
and "Replays triggered" panels update within the next 5-10s poll cycle.

> **Note:** this is a simplified stand-in for the target design's Observability container in
> `DESIGN.md` §2 (a real deployment would likely add alerting rules and more panels) — but it's
> the same tools (Prometheus + Grafana), not a mock.

### 5. Point it at the real webhook endpoint (once provided)

The webhook URL lives in the `subscription` table (`webhook_url` column), seeded by
`V2__seed_subscriptions.sql` with a placeholder. Once the real destination URL is provided:

```bash
docker compose exec postgres psql -U notifications -d notifications \
  -c "UPDATE subscription SET webhook_url = 'https://<real-url>' WHERE client_id = 'CLIENT001';"
```

No code change or redeploy is required — the Delivery Worker reads the URL from the database
on every poll cycle.

### 6. Run the tests

**Unit tests** (fast, no Docker needed — requires Java 21 on the host):

```bash
# from notification-service/
./mvnw test
```

39 tests: domain rules (`DeliveryAttempt`, `BackoffCalculator`), the application services
(`ReplayService`, `NotificationEventQueryService`, `DeliveryProcessingService`), the
`WebhookUrlValidator` SSRF checks, the webhook HTTP adapter against a plain
`com.sun.net.httpserver.HttpServer` (JDK-only, no mocking library), and the REST controller via
MockMvc.

**Integration tests** (needs Docker running — spins up real Postgres via Testcontainers):

```bash
# from notification-service/
./mvnw verify
```

11 additional `*IT` tests against real infrastructure, not mocks:

| Class | What it proves |
|---|---|
| `NotificationEventApiIT` | The 3 endpoints end-to-end through the real HTTP stack + real Postgres — list/filter, ownership (404 on cross-tenant), replay (202/409/404), auth (401) |
| `DeliveryWorkerIT` | The real scheduler → service → HTTP call → Postgres path, with WireMock standing in for the client's webhook: success, retry/backoff on failure, dead-letter after max retries, and replay re-entering the same path |
| `ConcurrentPollingIT` | Two simulated worker threads polling concurrently never claim the same due attempt twice — the `FOR UPDATE SKIP LOCKED` claim from `DESIGN.md` §5, which no mock-based test can verify |

`DeliveryWorkerIT` swaps only the `WebhookUrlValidator` bean for a permissive test double
(via `@TestConfiguration`), since WireMock necessarily binds to localhost and the real SSRF
guard correctly rejects that — every other component in the path (Postgres, the scheduler, the
outbound HTTP call, retry/backoff) is exercised for real. `./mvnw test` never touches these —
they only run under `verify`.

**Coverage report** (JaCoCo, merges unit + integration test runs, generated by `./mvnw verify`):

```bash
open target/site/jacoco/index.html   # macOS; xdg-open on Linux
```

| Layer | Instruction coverage |
|---|---|
| `domain.policy`, `domain.exception`, `infrastructure...scheduler` | 100% |
| `domain.model` | 98% |
| `infrastructure...persistence` (adapters) | 98% |
| `application.service` | 96% |
| `infrastructure.config` | 96% |
| `infrastructure...webhook` | 88% |
| `infrastructure...web` (controller, filter) | 86% |
| `infrastructure...persistence.entity` | 86% |
| **Overall** | **94%** (line coverage 94%, branch 78%) |

Running `./mvnw test` alone reports a lower number (~52%) since it only counts the unit-test
run — the persistence/entity/config/scheduler layers are exercised by the `*IT` tests above,
which only run under `verify`.

## Status

Design (Task 1) and security analysis (Task 3) complete. Task 2 implementation: all 3
self-service endpoints, webhook delivery with retry/backoff, and the 3 security mitigations are
implemented and tested. Challenge delivery on 2026-08-30.
