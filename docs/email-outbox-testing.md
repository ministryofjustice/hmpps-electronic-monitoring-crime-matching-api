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

### Permanent failure (4xx) → FAILED

When Notify returns a non-429 4xx the worker classifies the failure as permanent: it
marks the row `FAILED` and **returns without rethrowing**. SQS therefore receives a
successful acknowledgement and deletes the message immediately — no retries occur and
**no DLQ entry is produced**. `FAILED` is a terminal state; the row is not reprocessed
by the relay.

```bash
./scripts/notify-stub-mode.sh 400
# Trigger ingestion
./scripts/localstack-ingest-sample-email.sh
```

Verify the row reaches `FAILED` after one worker execution:

```bash
docker exec -i query-db psql -U postgres -d postgres -c \
  "select event_id, status, attempts, last_error from email_outbox order by created_at desc limit 5;"
```

Expected: row status `FAILED`, `attempts` = 1, `last_error` contains the Notify 400
response. The `emailsend_dlq` remains empty.

```bash
DLQ_URL=$(awslocal sqs get-queue-url --queue-name emailsend_dlq --query QueueUrl --output text)
awslocal sqs get-queue-attributes --queue-url "$DLQ_URL" \
  --attribute-names ApproximateNumberOfMessages
# Expected: ApproximateNumberOfMessages = 0
```

In production, `FAILED` rows trigger the `EmailOutboxFailed` Prometheus alert (see
`email-outbox-failed-alert.yaml`); investigate via `event_id` in OpenSearch/App
Insights and consult the runbook for remediation options.

> **Note**: `FAILED` rows cannot be redriven via `start-message-move-task` (there is no
> DLQ message). To re-attempt delivery, reset the row to `PENDING` in Postgres and let
> the relay re-dispatch:
> ```bash
> docker exec -i query-db psql -U postgres -d postgres -c \
>   "UPDATE email_outbox SET status = 'PENDING', claimed_at = NULL, claimed_by = NULL \
>    WHERE status = 'FAILED' AND event_id = '<event_id>';"
> ```

### Exhausted retries (5xx) → DLQ

To exercise the path where SQS exhausts its retry budget and dead-letters the message:

```bash
./scripts/notify-stub-mode.sh 500
# Trigger ingestion and wait for maxReceiveCount SQS deliveries.
```

After `maxReceiveCount` deliveries the message moves to the DLQ and the row is marked
`DEAD`:

```bash
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

## Ingestion idempotency — local verification

These steps verify that a redelivered SQS message for the same S3 object does not create
duplicate ingestion rows, batches, or outbox events, and that `publishMatchingRequest` is
retried only when the prior publish outcome is unknown.

### Prerequisites

Same as the happy-path setup above: DB, LocalStack, and notify-stub running, localstack
initialised, API running with `SPRING_PROFILES_ACTIVE=local`.

### 1. First ingestion (normal path)

```bash
bash scripts/localstack-ingest-sample-email.sh
```

Confirm a single attempt row with `PUBLISHED` state:

```bash
docker exec -i query-db psql -U postgres -d postgres -c \
"SELECT bucket, object_name, matching_publish_state, crime_batch_id FROM crime_batch_ingestion_attempt ORDER BY created_at DESC LIMIT 5;"
```

Expected: one row, `matching_publish_state = PUBLISHED`.

### 2. Duplicate redelivery — already published

Send the **same SQS message again** (same `bucketName` + `objectKey`, without clearing
the DB first) by re-running only the SQS send portion of the script, or by using the
LocalStack console.

Expected in API logs: `"Duplicate ingestion detected"` then `"Skipping publish — state is PUBLISHED"`.

```bash
BUCKET=police-emails
QUEUE_NAME=email
S3_KEY="samples/email-file.eml"
QUEUE_URL=$(awslocal sqs get-queue-url \
  --queue-name "$QUEUE_NAME" \
  --query QueueUrl \
  --output text)
awslocal sqs send-message \
  --queue-url "$QUEUE_URL" \
  --message-body "{
    \"Type\": \"Notification\",
    \"MessageId\": \"$(uuidgen)\",
    \"Message\": \"{\\\"notificationType\\\":\\\"Received\\\",\\\"receipt\\\":{\\\"action\\\":{\\\"type\\\":\\\"S3\\\",\\\"bucketName\\\":\\\"$BUCKET\\\",\\\"objectKeyPrefix\\\":\\\"\\\",\\\"objectKey\\\":\\\"$S3_KEY\\\"}}}\"
  }"
```

Confirm in Postgres: still only **one** `crime_batch_ingestion_attempt` row, one
`crime_batch`, one (or two if sending to the original sender too) `email_outbox` row.

```bash
docker exec -i query-db psql -U postgres -d postgres -c \
"SELECT bucket, object_name, matching_publish_state, crime_batch_id FROM crime_batch_ingestion_attempt ORDER BY created_at DESC LIMIT 5;"
docker exec -i query-db psql -U postgres -d postgres -c \
"SELECT * FROM crime_batch ORDER BY created_at DESC LIMIT 5;"
docker exec -i query-db psql -U postgres -d postgres -c \
"SELECT DISTINCT crime_batch_id FROM email_outbox LIMIT 5;"

```

### 3. Transient SNS failure → state stays PENDING_OR_UNCONFIRMED → retry on redelivery

Switch the notify stub to return 500:

```bash
./scripts/notify-stub-mode.sh 500-then-201
```

Clear the DB and trigger a fresh ingestion with a new object key (or re-run the full
script). On the first delivery the SNS publish fails; state remains `PENDING_OR_UNCONFIRMED` (inspect with breakpoints). SQS
redelivers the same message → duplicate path detects `PENDING_OR_UNCONFIRMED` → retries publish →
succeeds → marks `PUBLISHED`.

Switch back when done:

```bash
./scripts/notify-stub-mode.sh 201
```

Confirm final state:

```bash
docker exec -i query-db psql -U postgres -d postgres -c \
"SELECT * FROM crime_batch ORDER BY created_at DESC LIMIT 5;"
```

Expected: `matching_publish_state = PUBLISHED`, still only one `crime_batch` row.

### 4. Manually reset to PENDING_OR_UNCONFIRMED to re-exercise retry

To re-run the retry path without re-ingesting, reset the state in Postgres:

```bash
docker exec -i query-db psql -U postgres -d postgres -c \
"UPDATE crime_batch_ingestion_attempt SET matching_publish_state = 'PENDING_OR_UNCONFIRMED' WHERE matching_publish_state = 'PUBLISHED';"
```

Then resend the same SQS message. The duplicate path will fire and retry publish.

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

