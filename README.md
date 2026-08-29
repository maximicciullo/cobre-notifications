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

- **Language/Framework**: Java 21 + Spring Boot 3 (hexagonal architecture)
- **Database**: PostgreSQL
- **Delivery**: outbox pattern + scheduled poller, retry with exponential backoff
- **Containerization**: Docker + Docker Compose (app + Postgres)

## How to run

_(Pending — filled in once the Task 2 code is ready)_

## Status

In progress — challenge delivery on 2026-08-30.
