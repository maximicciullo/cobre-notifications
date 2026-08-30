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

- **Language/Framework**: Java 21 + Spring Boot 3.5.3 (hexagonal architecture)
- **Database**: PostgreSQL 16
- **Delivery**: outbox pattern + scheduled poller, retry with exponential backoff
- **Containerization**: Docker + Docker Compose (app + Postgres)

## How to run

```bash
cd notification-service
docker compose up --build
```

This starts Postgres, runs the Flyway migrations, and seeds the app with the 10 events from
`challenge/notification_events.json` plus 3 demo subscriptions (one per client in that file).
The API is then available at `http://localhost:8082`.

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

### How to point it at the real endpoint

The webhook URL lives in the `subscription` table (`webhook_url` column), seeded by
`V2__seed_subscriptions.sql` with a placeholder. Once the real destination URL is provided:

```bash
docker compose exec postgres psql -U notifications -d notifications \
  -c "UPDATE subscription SET webhook_url = 'https://<real-url>' WHERE client_id = 'CLIENT001';"
```

No code change or redeploy is required — the Delivery Worker reads the URL from the database
on every poll cycle.

### Running the tests

```bash
cd notification-service
./mvnw test
```

31 tests: domain rules (`DeliveryAttempt`, `BackoffCalculator`), the `ReplayService` A01
ownership checks, the `WebhookUrlValidator` A10/SSRF checks, the webhook HTTP adapter against a
WireMock server, and the REST controller via MockMvc.

## Status

Design (Task 1) and security analysis (Task 3) complete. Task 2 implementation: all 3
self-service endpoints, webhook delivery with retry/backoff, and the 3 security mitigations are
implemented and tested. Challenge delivery on 2026-08-30.
