package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch

data class CrimeBatchDto(
  val id: String,
  val crimes: List<CrimeDto>,
  val policeForce: String,
) {
  constructor(crimeBatch: CrimeBatch) : this(
    id = crimeBatch.id,
    policeForce = crimeBatch.policeForce.name,
    crimes = crimeBatch.crimes.map {
      CrimeDto(it)
    },
  )
}
