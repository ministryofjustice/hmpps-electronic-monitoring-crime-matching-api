package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch

data class CrimeBatchDto(
  val id: String,
  val policeForce: String
) {
  constructor(crimeBatch: CrimeBatch): this(
    id = crimeBatch.id,
    policeForce = crimeBatch.policeForce
  )
}
