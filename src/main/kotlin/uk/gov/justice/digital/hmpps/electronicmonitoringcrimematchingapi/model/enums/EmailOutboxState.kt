package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class EmailOutboxState {
  PENDING, // About to attempt to publish (it's possible that we have published but were unable to record it)
  PUBLISHED, // Definitely published, but we don't know what happened downstream.
  FAILED, // Failed but we plan to retry (not yet implemented - currently is a terminal state)
  DEAD, // Failed and we don't plan to retry (not yet implemented)
}
