# Email Outbox: Local & Dev Testing Guide

How to exercise the transactional-outbox email flow (ingest → outbox → relay →
`emailsend` SQS worker → GOV.UK Notify) with automated tests and documented manual
runs.

See failure investigation steps in
[email-notifications-dlq-runbook.md](email-notifications-dlq-runbook.md).

---
## Manual test — Local

Local defaults to `notify.enabled: false`, so no real send happens. To exercise the
full send + retry + DLQ locally, point Notify at a local WireMock stub and enable it.
The `emailsend` queue and its DLQ are **auto-created** by the hmpps-sqs starter on
startup (like the `email` queue) — no manual queue creation needed.

### One-time setup

```bash
# Start infra (Postgres, LocalStack sns/sqs/s3, hmpps-auth)
docker compose up -d db localstack notify-stub

# S3 bucket (email queue + emailsend queue are auto-created by the app)
./scripts/localstack-init.sh

# Local Notify stub on 8093 is compose-managed and starts in 201 mode
curl -s "http://localhost:8093/__admin/mappings" | jq '.mappings[] | {method: .request.method, url: .request.url}'

# Optional: switch stub response mode quickly (201, 400, 500, or 500-then-201)
./scripts/notify-stub-mode.sh 201
```

### Local config overrides (`application-local.yml` or env)
- `notify.enabled: true`, `notify.base-url: http://localhost:8093`, template ids as in `application-test.yml`.
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

# Notify calls observed (normally one on happy path; contract is at-least-once submit)
curl -s http://localhost:8093/__admin/requests/count \
  -H 'Content-Type: application/json' \
  -d '{"method":"POST","url":"/v2/notifications/email"}'
```

Expected on happy path: one row transitioning `PENDING → SENT`, `attempts` = 0/1, Notify count typically = 1.
Design note: submit semantics are at-least-once; dedupe of end-user delivery relies on
Notify `reference = event_id` behavior:
https://docs.notifications.service.gov.uk/java.html#reference-required

### Transient failure → retry
Reconfigure the stub to return `500`, re-ingest, watch `attempts` climb across
relay/worker cycles, then flip back to `201` and confirm the row settles to `SENT`
with a single successful send.

```bash
./scripts/notify-stub-mode.sh 500
# trigger ingestion and observe retries
./scripts/notify-stub-mode.sh 201
```

If you want to re-run the same test without re-ingesting, reset the outbox row to PENDING (or wait until the claim is released):
```bash
docker exec -i query-db psql -U postgres -d postgres -c \
"UPDATE email_outbox SET status = 'PENDING', claimed_at = NULL, claimed_by = NULL WHERE status = 'CLAIMED';"
```

### Permanent failure → DLQ

```bash
# Stub returns 400 for all sends, then ingest.
./scripts/notify-stub-mode.sh 400

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

Because the worker is idempotent after persistence (`event_id` + terminal-status guard), replay is
safe operationally. Submit semantics remain at-least-once; Notify `reference` dedupe is relied on
to avoid duplicate end-user delivery:
https://docs.notifications.service.gov.uk/java.html#reference-required

---

## Manual test — Dev

- **Happy path** — forward a valid police email to the dev mailbox; confirm one
  GOV.UK Notify send (Notify dashboard) and one `email_outbox` row = SENT in dev
  RDS. Notify's built-in 5×/5-min retry handles downstream delivery.
- **Retry/DLQ** — temporarily set an invalid Notify key (or a permanent-fail
  template) to force a failure; observe the `emailsend` DLQ alert; restore config and
  redrive per the runbook — verify expected behavior with Notify `reference` dedupe.
- **Observability** — confirm `email.outbox.event` counters in Prometheus/Grafana
  and that the DLQ alert mirrors the existing `email-notifications-dlq` panels.

