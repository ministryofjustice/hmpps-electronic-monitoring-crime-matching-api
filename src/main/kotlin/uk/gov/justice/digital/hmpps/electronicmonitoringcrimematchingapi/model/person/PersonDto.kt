package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaPersonDTO
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.deviceactivation.DeviceActivationDto

data class PersonDto(
  val personId: String,
  val personName: String,
  val nomisId: String?,
  val dateOfBirth: String?,
  val address: String?,
  val deviceActivations: List<DeviceActivationDto>?,
) {
  constructor(dto: AthenaPersonDTO) : this(
    personId = dto.personId,
    personName = dto.personName,
    nomisId = dto.uIdNomis,
    dateOfBirth = dto.uDob,
    address = "${dto.street} ${dto.city} ${dto.zip}",
    deviceActivations = null,
  )
}
