package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

/**
 * Tracks whether the matching-notification SNS publish was confirmed for an ingestion attempt.
 *
 * - [UNKNOWN]        – Default on creation, or set when a prior publish attempt's outcome was not
 *                      persisted (e.g. the node crashed after a successful SNS call). Duplicate
 *                      deliveries will retry [publishMatchingRequest] to guarantee at-least-once.
 * - [PUBLISHED]      – SNS publish confirmed; duplicate deliveries safely skip re-publishing.
 * - [NOT_APPLICABLE] – The ingestion outcome was FAILED or ERROR; no publish is ever needed.
 */
enum class MatchingPublishState {
  UNKNOWN,
  PUBLISHED,
  NOT_APPLICABLE,
}
