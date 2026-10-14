package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

data class CrimeDetails(
  val batchId: String,
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
