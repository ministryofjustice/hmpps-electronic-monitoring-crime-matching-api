# Email Outbox: Flow Diagrams

## End-to-End Flow (Happy & Sad Paths)

```mermaid
flowchart TD
    A([SQS email queue\nmessage arrives]) --> B[EmailListener\n.receiveEmailNotification]

    B --> C[Fetch email file\nfrom S3]
    C --> D[Parse email /\nextract EmailData]

    D --> E{Parse or\nvalidation error?}
    E -- Yes --> F[Throw exception]
    F --> G{email queue\nretry count?}
    G -- "< maxReceiveCount" --> B
    G -- ">= maxReceiveCount" --> H([email DLQ\nmanual review])

    E -- No --> I

    subgraph tx["Single transaction — commit atomically or rollback both"]
        I[processEmail\npersist ingestion outcome] --> J[EmailOutboxService.enqueue\nresolve recipients\nwrite one PENDING row per recipient]
    end

    J --> K[COMMIT]
    K --> L{SUCCESSFUL\nor PARTIAL?}
    L -- Yes --> M[publishMatchingRequest\nSNS topic]
    L -- No --> N([EmailListener done])
    M --> N

    K -.->|"@Scheduled every 5s\n(configurable)"| relay

    subgraph relay["EmailOutboxRelay.dispatchPending — runs on all replicas concurrently"]
        R1[reclaimExpired\nCLAIMED rows back to PENDING] --> R2[claimBatch\nFOR UPDATE SKIP LOCKED]
        R2 --> R3[mark rows CLAIMED\npublish eventId to emailsend queue]
    end

    R3 --> Q([emailsend SQS queue])
    Q --> W[EmailSendListener\n.receiveEmailSend]

    W --> WA{Row found?}
    WA -- No --> WB([Log & skip\nSQS deletes message])
    WA -- Yes --> WC{Terminal status?\nSENT / FAILED / DEAD}
    WC -- Yes --> WD([No-op — idempotent\nSQS deletes message])
    WC -- No --> WE["sendEmail\nsingle recipient\nreference = event_id"]

    WE --> WF{GOV.UK Notify\nresponse}
    WF -- "2xx" --> WG([markSent\nSENT — terminal\nSQS deletes message])
    WF -- "4xx not 429\npermanent" --> WH([markFailed\nFAILED — terminal\nno rethrow, SQS deletes])
    WF -- "429 / 5xx\ntransient" --> WI[markRetry\nrethrow exception]
    WI --> WJ{ApproximateReceiveCount\n>= maxReceiveCount?}
    WJ -- No --> Q
    WJ -- Yes --> WK[markDead\nrethrow exception]
    WK --> WL([emailsend DLQ\nmanual review per runbook])

    style WG fill:#2d7d46,color:#fff
    style WH fill:#8b4513,color:#fff
    style WL fill:#8b0000,color:#fff
    style H  fill:#8b0000,color:#fff
    style WB fill:#555,color:#fff
    style WD fill:#555,color:#fff
```

---

## `email_outbox` Row State Machine

```mermaid
stateDiagram-v2
    direction LR
    [*] --> PENDING: enqueue()\none row per recipient\natomic with ingest tx

    PENDING --> CLAIMED: relay claimBatch()\nFOR UPDATE SKIP LOCKED

    CLAIMED --> PENDING: reclaimExpired()\nlease timed out\ncrash / publish failure

    CLAIMED --> SENT: Notify 2xx\nmarkSent()

    CLAIMED --> FAILED: Notify 4xx not 429\nmarkFailed()\nno retry, SQS deletes

    CLAIMED --> CLAIMED: transient error\nmarkRetry()\nSQS redelivers

    CLAIMED --> DEAD: final attempt\nmarkDead()\nmessage to DLQ

    SENT --> [*]
    FAILED --> [*]
    DEAD --> [*]
```

---

## Behavior per partial-failure scenario

| Scenario | What happens |
|---|---|
| All recipients succeed | Each recipient's row → `SENT` independently |
| All fail (transient) | Each row retries independently; on final attempt → `DEAD` + DLQ |
| All fail (Notify 4xx) | Each row → `FAILED` immediately; no retry storm |
| **Some succeed, some fail** | Succeeded rows stay `SENT` (terminal no-op on redelivery); only failed rows retry — **no duplicate emails** |
| Relay crash mid-batch | Leased rows reclaimed to `PENDING` after `leaseTimeout` |
| SQS redelivery of a `SENT` row | Terminal-status guard no-ops; submission contract is at-least-once, and Notify `reference = event_id` dedupes at provider (https://docs.notifications.service.gov.uk/java.html#reference-required) |
| DLQ replay | Idempotent worker makes replay safe; investigate via `event_id` per runbook |

See [email-outbox-testing.md](email-outbox-testing.md) for how to test each path.



