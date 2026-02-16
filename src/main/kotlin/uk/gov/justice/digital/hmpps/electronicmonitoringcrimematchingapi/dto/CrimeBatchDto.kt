package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class CrimeBatchDto(
  val id: String,
  val batchId: String,
  val crimes: List<CrimeDto>,
)
