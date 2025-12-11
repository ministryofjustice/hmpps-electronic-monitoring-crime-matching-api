package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch

data class CrimeBatchDto(
  val id: String,
  val batchId: String,
  val crimes: List<CrimeDto>,
) {
  constructor(crimeBatch: CrimeBatch) : this(
    id = crimeBatch.id.toString(),
    batchId = crimeBatch.batchId,
    crimes = crimeBatch.crimeVersions.map {
      CrimeDto(it)
    },
  )
}
