# Email Outbox: Local & Dev Testing Guide

How to exercise the transactional-outbox email flow (ingest → outbox → relay →
`emailsend` SQS worker → GOV.UK Notify) with automated tests and documented manual
runs.

See the design in [email-outbox-plan.md](email-outbox-plan.md) and failure
investigation steps in [email-notifications-dlq-runbook.md](email-notifications-dlq-runbook.md).

---

## 1. Automated tests

### Unit (JUnit + Mockito)
Mirror the style of `CrimeBatchServiceTest` / `MatchingNotificationServiceTest`.

- **Payload round-trip** (`EmailOutboxPayloadMapperTest`) — an
  `EmailIngestionOutcome` serialises to JSON and back into an equivalent outcome
  the existing `EmailNotificationService.sendEmails` accepts (per status).
- **Enqueue** (`EmailOutboxServiceTest`) — `enqueue` persists a `PENDING` row with
  a generated `event_id`, the serialised payload, and a parsed `crime_batch_id`
  (null for FAILED/ERROR).
- **Claim / lifecycle** — `claimBatch` marks rows `CLAIMED`; `markSent` /
  `markRetry` / `markDead` transition correctly and update `attempts`.
- **Relay** (`EmailOutboxRelayTest`) — `dispatchPending()` reclaims expired leases,
  claims a batch, and publishes each `event_id`.
- **Worker** (`EmailSendListenerTest`) — terminal-status no-op; success → `markSent`;
  failure re-throws and marks retry/dead based on `ApproximateReceiveCount`.
- **Metrics** — `email.outbox.event` counters increment on each transition.

### Integration (LocalStack + Notify WireMock)
Extend `integration/listener/EmailListenerTest`. Because sending is now
asynchronous, assert with Awaitility:

```kotlin
await().untilAsserted { notifyMockServer.verifyEmailSentTo("test@email.com", 1) }
```

| Scenario | Assertion |
|---|---|
| Happy path exactly-once | `email_outbox` row PENDING→SENT; Notify called once |
| Idempotency on redelivery | Re-publish same `event_id` → Notify still called once |
| Atomicity (iff ingest succeeded) | Spy `crimeBatchRepository` to throw after save → no outbox row, no Notify call |
| Transient retry | Notify 500 then 201 (WireMock Scenario) → row SENT, `attempts` incremented |
| Permanent failure → DLQ | Notify 400 → message on `emailsend` DLQ, row DEAD |
| FAILED/ERROR path | Invalid batch → outbox row keyed on `event_id` (no `crime_batch_id`), sent once |

**Determinism helpers**
- The relay exposes a public `dispatchPending()` so tests can trigger dispatch
  directly instead of waiting on `@Scheduled`.
- `email.outbox.relay.interval-ms` can be shortened in `application-test.yml`.
- Extend `NotifyMockServer` with `stubSendEmailServerError()` (500) and
  `stubSendEmailBadRequest()` (400) plus a fail-then-201 WireMock Scenario.

---

## 2. Manual test — Local

Local defaults to `notify.enabled: false`, so no real send happens. To exercise the
full send + retry + DLQ locally, point Notify at a local WireMock stub and enable it.
The `emailsend` queue and its DLQ are **auto-created** by the hmpps-sqs starter on
startup (like the `email` queue) — no manual queue creation needed.

### One-time setup

```bash
# Start infra (Postgres, LocalStack sns/sqs/s3, hmpps-auth)
docker compose up -d db localstack hmpps-auth

# S3 bucket (email queue + emailsend queue are auto-created by the app)
./scripts/localstack-init.sh

# Local Notify stub on 8092 (returns 201 for POST /v2/notifications/email)
docker run -d --name notify-stub -p 8092:8080 wiremock/wiremock
# Add a mapping: POST /v2/notifications/email -> 201 (mirror NotifyMockServer body)
```

### Local config overrides (`application-local.yml` or env)
- `notify.enabled: true`, `notify.base-url: http://localhost:8092`, template ids as in `application-test.yml`.
- The `emailsend` queue (with `dlqName`) is already added under `hmpps.sqs.queues`.

### Happy path

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun

# Trigger ingestion (also clears app tables)
./scripts/localstack-ingest-sample-email.sh
```

Verify:

```bash
# Outbox row: PENDING then SENT once the relay + worker run
docker exec -i query-db psql -U postgres -d postgres -c \
  "select event_id, crime_batch_id, status, attempts from email_outbox order by created_at desc limit 5;"

# Exactly one Notify call
curl -s http://localhost:8092/__admin/requests/count \
  -H 'Content-Type: application/json' \
  -d '{"method":"POST","url":"/v2/notifications/email"}'
```

Expected: one row transitioning `PENDING → SENT`, `attempts` = 0/1, Notify count = 1.

### Transient failure → retry
Reconfigure the stub to return `500`, re-ingest, watch `attempts` climb across
relay/worker cycles, then flip back to `201` and confirm the row settles to `SENT`
with a single successful send.

### Permanent failure → DLQ

```bash
# Stub returns 400 for all sends, then ingest.
# After maxReceiveCount deliveries the message moves to the DLQ:
DLQ_URL=$(awslocal sqs get-queue-url --queue-name emailsend_dlq --query QueueUrl --output text)
awslocal sqs get-queue-attributes --queue-url "$DLQ_URL" \
  --attribute-names ApproximateNumberOfMessages
awslocal sqs receive-message --queue-url "$DLQ_URL"
```

Expected: outbox row `DEAD`, one message on `emailsend_dlq` carrying `event_id`.

### Replay (parity with current DLQ process)

```bash
awslocal sqs start-message-move-task \
  --source-arn arn:aws:sqs:eu-west-2:000000000000:emailsend_dlq
```

Because the worker is idempotent (`event_id` + terminal-status guard), replay is
safe and yields exactly one email.

---

## 3. Manual test — Dev

- **Happy path** — forward a valid police email to the dev mailbox; confirm one
  GOV.UK Notify send (Notify dashboard) and one `email_outbox` row = SENT in dev
  RDS. Notify's built-in 5×/5-min retry handles downstream delivery.
- **Retry/DLQ** — temporarily set an invalid Notify key (or a permanent-fail
  template) to force a failure; observe the `emailsend` DLQ alert; restore config and
  redrive per the runbook — verify no duplicate email (idempotency).
- **Observability** — confirm `email.outbox.event` counters in Prometheus/Grafana
  and that the DLQ alert mirrors the existing `email-notifications-dlq` panels.

---

## 4. Cleanup (local)

```bash
docker rm -f notify-stub
docker compose down
```

