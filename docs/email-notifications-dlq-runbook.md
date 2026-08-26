# Runbook: Investigating Email Notifications DLQ Messages

## Purpose

This runbook describes the process for investigating messages that have failed to
process and have been moved to the shared Dead Letter Queue.
An alert will notify the team when a message has been moved to the DLQ.

> **Note**
> This runbook applies to the single shared DLQ used by this service:
> - `email-notifications-dlq`
>
> The queue can contain different message schemas:
> - Ingestion failure payloads (S3-received email processing)
> - `EMAILSEND` worker failures after exhausted retries
>
> Redrive/replay is a **manual operational action** after investigation; there is no automatic in-app DLQ redrive.

---

## `FAILED` outbox rows (Notify 4xx — no DLQ entry)

`FAILED` rows arise when GOV.UK Notify returns a permanent non-429 4xx response (e.g.
invalid API key, unknown template ID, or a malformed request). The worker marks the row
`FAILED` and returns without rethrowing, so SQS deletes the message after one delivery —
**no `email-notifications-dlq` entry is produced**.

The `EmailOutboxFailed` Prometheus alert fires on any such event. To investigate:

1. Note the `event_id` from the alert labels or from a Postgres query:
   ```sql
   SELECT event_id, status, attempts, last_error, updated_at
   FROM email_outbox
   WHERE status = 'FAILED'
   ORDER BY updated_at DESC
   LIMIT 20;
   ```
2. Search OpenSearch or App Insights for the `event_id` to find the full Notify error
   response (status code + body logged at `WARN` level by the worker).
3. Common causes and remediation:

   | Notify error | Cause | Action |
   |---|---|---|
   | 403 `invalid_token` | `NOTIFY_API_KEY` secret wrong or rotated | Rotate the secret; reset row to `PENDING` |
   | 400 `BadRequestError` (template) | `NOTIFY_*_TEMPLATE_ID` misconfigured | Correct the template ID config; reset row |
   | 400 `ValidationError` (email address) | Recipient address rejected by Notify | Investigate upstream data; do not redrive |

4. To re-attempt delivery after fixing the root cause, reset the row to `PENDING`:
   ```sql
   UPDATE email_outbox
   SET status = 'PENDING', claimed_at = NULL, claimed_by = NULL, last_error = NULL
   WHERE status = 'FAILED'
   AND event_id = '<event_id>';
   ```
   The relay will re-claim and re-dispatch on its next cycle.

---
# Investigation Process

## Step 1: Retrieve the DLQ Message

1. Log in to the AWS Console.
2. Navigate to the email notifications DLQ in Amazon SQS.
3. Poll for messages using **Send and receive messages**.
4. Open the failed message to retrieve the DLQ message ID.

The DLQ Message ID is the primary identifier used to correlate failures across Application Insights and OpenSearch.

### Distinguish the Message Schema First

Because `email-notifications-dlq` is shared, first identify whether the failed
message is an ingestion payload or an `EMAILSEND` payload.

- Ingestion payload indicators: S3 metadata fields such as bucket/object key and
  email parsing fields (subject/forwarding address/original sender).
- `EMAILSEND` payload indicators: outbox/event fields such as `event_id` and
  send-attempt context from the outbox worker flow.

### Information Available in the Message Payload

The DLQ payload schema depends on the failing flow:

- Ingestion failures commonly include:
  - Original SQS Message ID
  - S3 Object Key
  - S3 Bucket Name
  - Email Subject
  - Email Forwarding Address
  - Email Original Sender
- `EMAILSEND` exhausted-retry failures commonly include identifiers that let you
  correlate to `email_outbox` (for example `event_id`) and worker processing logs.

Use the schema type to choose the right investigation path (ingestion parsing/
validation vs outbox send/retry behavior).

---

## Step 2: Review the Failure in Application Insights/OpenSearch

### Review via Application Insights Logs
1. Open [**Application Insights**](https://portal.azure.com/#browse/microsoft.insights%2Fcomponents)
2. Navigate to **Logs**.
3. Query on exceptions:
   1. ``` exceptions | where cloud_RoleName == 'hmpps-electronic-monitoring-crime-matching-api' ```
   2. Add ```| where details contains "18532b58-d1e9-4280-982b-5d787d64614e"``` to filter on a specific DLQ Message ID
4. View the details section for more information on the exception including stack trace.
5. You can also take the **operation_Id** or the **operation_ParentId** from here and use it in the **Search** window to access the end-to-end transaction.

### Review via Application Insights Failures

1. Open [**Application Insights**](https://portal.azure.com/#browse/microsoft.insights%2Fcomponents)
2. Navigate to **Failures**.
3. Filter on the **Roles** by deselecting all roles and selecting the following:

```text
hmpps-electronic-monitoring-crime-matching-api
```

4. Search for **RECEIVE** operations.

5. Select the operation then **Drill into…** and browse the samples for the relevant message using the DLQ message ID.

### Reviewing Exception Details

The failure reason is typically visible within the **Call Stack** section of an event.

Look for exceptions such as:

```text
AsyncAdapterBlockingExecutionFailedException
```

The DLQ Message ID is generally included within the exception text, for example:

```text
Listener failed to process messages 18532b58-d1e9-4280-982b-5d787d64614e
```

Capture:

- Exception type
- Exception message
- Stack trace
- Timestamp

These details will usually indicate the underlying cause of the failure.

---
## OpenSearch

1. Open [OpenSearch Dashboards](https://app-logs.cloud-platform.service.justice.gov.uk/_dashboards/app/home#/).
2. Select the live_kubernetes_cluster-* index pattern.
3. Add filters for namespace and container:
   1. ```kubernetes.namespace_name: hmpps-electronic-monitoring-crime-matching-dev```
   2. ```kubernetes.container_name: hmpps-electronic-monitoring-crime-matching-api```
4. Search using the DLQ Message ID obtained in Step 1.

### Common Error Messages

Failed listener executions typically generate logs similar to:

```text
Caused by: io.awspring.cloud.sqs.listener.ListenerExecutionFailedException:
Listener failed to process messages 18532b58-d1e9-4280-982b-5d787d64614e
```

or:

```text
Error processing message 18532b58-d1e9-4280-982b-5d787d64614e
```

### Identify the Root Cause

Review log entries immediately before and after the listener failure.

The surrounding log messages typically contain the actual cause of the failure.

#### Example: Invalid Email Subject

```text
Caused by: jakarta.validation.ValidationException:
Invalid email subject
```

In this scenario, the message failed because the email subject did not match the expected validation rules.

---

## Step 4: Determine the Root Cause

Using the information gathered from:

- DLQ message payload
- Application Insights exceptions
- OpenSearch logs

categorise the failure.

### Validation Errors

Examples:

```text
Invalid email subject
No redirect email
Invalid redirect email
Invalid sender email
```

**Action**
- Verify the sender addresses are from valid sources.
- Request the sender resubmits the corrected email.

### File Processing Errors

Examples:

```text
Unable to retrieve S3 object
SQS message malformed
```

**Action**

- Verify the S3 object exists.
- Verify the connection to S3 is functioning correctly.
- Verify the SQS message structure.

---

## Information to Capture

For every investigation, record:

```text
DLQ Message ID:
Original SQS Message ID:
S3 Bucket Name:
S3 Object Key:
Email Subject:
Forwarding Address:
Original Sender:
Exception Type:
Exception Message:
Root Cause:
Resolution:
Investigator:
Date:
```
---
## Summary
When investigating a DLQ message:

1. Retrieve the message from the DLQ and record the DLQ Message ID.
2. Extract metadata from the message payload (S3 details, email details and original SQS Message ID).
3. Locate the failed transaction in Application Insights.
4. Review the Call Stack for the failure reason.
5. Alternatively search OpenSearch using the DLQ Message ID and review the logs to identify the root cause.
6. Categorise the issue and determine the appropriate remediation or escalation path.