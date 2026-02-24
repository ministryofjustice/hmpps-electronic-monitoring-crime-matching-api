package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class CrimeBatchResponse(
  val id: String,
  val batchId: String,
  val crimes: List<CrimeResponse>,
)
