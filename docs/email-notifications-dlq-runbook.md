# Runbook: Investigating Email Notifications DLQ Messages

## Purpose

This runbook describes the process for investigating email ingestion messages that have failed to process and have been moved to the Dead Letter Queue.
An alert will notify the team when a message has been moved to the DLQ.

---
# Investigation Process

## Step 1: Retrieve the DLQ Message

1. Log in to the AWS Console.
2. Navigate to the email notifications DLQ in Amazon SQS.
3. Poll for messages using **Send and receive messages**.
4. Open the failed message to retrieve the DLQ message ID.

The DLQ Message ID is the primary identifier used to correlate failures across Application Insights and OpenSearch.

### Information Available in the Message Payload

The `Message` field of the DLQ payload will have the following information:

- Original SQS Message ID
- S3 Object Key
- S3 Bucket Name
- Email Subject
- Email Forwarding Address
- Email Original Sender

These values can assist in determining the root cause of the failure.

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