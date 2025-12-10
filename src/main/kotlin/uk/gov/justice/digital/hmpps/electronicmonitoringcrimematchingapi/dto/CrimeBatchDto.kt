package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import java.util.UUID

data class CrimeBatchDto(
  val id: UUID,
  val batchId: String,
  val crimes: List<CrimeDto>,
) {
  constructor(crimeBatch: CrimeBatch, crimes: List<CrimeVersion>) : this(
    id = crimeBatch.id,
    batchId = crimeBatch.batchId,
    crimes = crimes.map {
      CrimeDto(it)
    },
  )
}
