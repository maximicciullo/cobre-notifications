# SECURITY — Task 3: OWASP Top 10 Analysis

The self-service API and the webhook delivery mechanism are both reachable over the public
internet, so this analysis picks **3 OWASP Top 10 (2021) vulnerabilities** that are directly
created by this specific design — not a generic checklist. All three have a concrete mitigation
actually implemented in the Task 2 code (see "Where" under each), not just described here.

---

## 1. A01:2021 — Broken Access Control

**Why it applies here:** the platform is multi-tenant — every `NotificationEvent` belongs to
exactly one `client_id` (see `DESIGN.md` §4). The self-service API exposes 3 endpoints that all
take an `notification_event_id` directly from the request path, with no inherent guarantee the
caller is asking about *their own* data.

**Attack scenario:** event IDs are predictable/sequential (`EVT001`, `EVT002`, ...). An
authenticated client (or anyone who guesses/enumerates an ID) calls
`GET /notification_events/EVT003` or `POST /notification_events/EVT003/replay` — an event that
actually belongs to a different client. Without an ownership check, this is a classic **IDOR**
(Insecure Direct Object Reference): the attacker can read another client's transaction content,
or worse, trigger a redelivery of someone else's notification to wherever *they* control.

**Mitigation:**
- Every request is authenticated, and the caller's `client_id` comes from the auth context —
  **never** from the request body/path.
- `GET /notification_events` is implicitly scoped: `WHERE client_id = :authenticatedClientId`,
  always, not an optional filter.
- `GET /{id}` and `POST /{id}/replay` load the event, then explicitly check
  `event.client_id == authenticatedClientId` **before** returning or acting on it. On mismatch,
  return `404 Not Found` (not `403`) — a `403` would confirm the ID exists and belongs to
  someone else, leaking information about other tenants' data.

**Where:** enforced once in the application/use-case layer (hexagonal core), not just in the
controller — so it can't be bypassed by adding a new adapter later.

---

## 2. A03:2021 — Injection

**Why it applies here:** `GET /notification_events` takes client-supplied filter parameters
(event creation date range, `delivery_status`) that feed directly into a database query.

**Attack scenario:** if filters were built by concatenating raw query-string values into SQL
(`"...WHERE status = '" + status + "'"`), an attacker could inject `' OR '1'='1` to read every
client's events regardless of the `client_id` scope from A01, or attempt destructive SQL. This
is the most common way tenant isolation gets silently broken in practice — injection and access
control failures compound.

**Mitigation:**
- All queries go through Spring Data JPA (derived query methods / `@Query` with **named
  parameters**) — string concatenation into a query is never used anywhere in the codebase.
- `delivery_status` is validated against a fixed enum (`PENDING`, `COMPLETED`, `FAILED`) at the
  DTO/controller boundary before it reaches the query layer; anything else is rejected with
  `400 Bad Request`.
- Date filters are parsed into typed values (`Instant`) at the boundary — malformed input fails
  closed with `400`, it never reaches the database as a raw string.

**Where:** repository layer (Spring Data JPA) + request validation (Bean Validation) on the
inbound DTO, both in the adapters layer, keeping the domain itself free of raw query strings.

---

## 3. A10:2021 — Server-Side Request Forgery (SSRF)

**Why it applies here:** this is the risk most unique to this specific system. The Delivery
Worker makes an **outbound HTTPS call to a URL that is, by design, configurable per client**
(`Subscription.webhook_url` — see `DESIGN.md` §4). Any service that fetches a URL supplied
(directly or indirectly) by a user is a potential SSRF vector.

**Attack scenario:** a client sets (or the platform seeds) a `webhook_url` pointing at
`http://169.254.169.254/latest/meta-data/` (cloud instance metadata service) or
`http://localhost:8080/actuator/env` (our own internal Actuator). Running inside the private
network, the Delivery Worker would happily make that request on the attacker's behalf — turning
our own delivery mechanism into a proxy into infrastructure that was never meant to be
internet-reachable.

**Mitigation** — a URL validator runs **before every outbound call**, not just at
subscription-creation time (since DNS can change after validation — "TOCTOU"/DNS-rebinding):
1. Scheme must be `https` — no plain `http`.
2. Resolve the hostname and reject if it maps to a private/reserved range: RFC1918
   (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`), loopback, and link-local
   (`169.254.0.0/16`, which covers the cloud metadata service).
3. Re-validate the resolved IP **immediately before connecting**, not only when the URL was
   first saved — prevents DNS-rebinding (a hostname that resolves to a public IP at
   registration time but a private one at request time).
4. No automatic following of redirects to an unvalidated location; if a redirect must be
   followed, the target goes through the same validation first.
5. Strict connect/read timeouts, so a captured internal endpoint can't hang a worker slot.

**Where:** a dedicated adapter-level component (e.g. `WebhookUrlValidator`) invoked by the
Delivery Worker's HTTP adapter immediately before every request — enforced at the edge of the
hexagon, not scattered through business logic.

---

## Testing

Each mitigation above gets a unit test that proves the *bad* case is actually blocked, not just
the happy path — e.g. requesting another client's event returns `404`, a malformed
`delivery_status` returns `400`, and a webhook URL pointing at `169.254.169.254` or
`http://localhost` is rejected before any HTTP call is attempted. See the testing scope in
`DESIGN.md` §5.
