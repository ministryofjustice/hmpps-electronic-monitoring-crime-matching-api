package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class PublishMatchingState {
  PENDING_OR_UNCONFIRMED, // May or may not have been published, but we either don't know or can't know. Publishing could be in progress.
  PUBLISHED, // Definitely published, but we don't know what happened downstream.
}
