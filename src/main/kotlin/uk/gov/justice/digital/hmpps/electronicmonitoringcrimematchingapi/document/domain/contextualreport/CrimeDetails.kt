package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import java.time.Instant

data class CrimeDetails(
  val batchId: String,
  val proximityAlertCreatedAt: Instant,
  val reference: String,
  val type: String,
) {
  init {
    require(batchId.isNotBlank()) {
      "'batchId' cannot be blank"
    }

    require(reference.isNotBlank()) {
      "'reference' cannot be blank"
    }

    require(type.isNotBlank()) {
      "'type' cannot be blank"
    }
  }
}
