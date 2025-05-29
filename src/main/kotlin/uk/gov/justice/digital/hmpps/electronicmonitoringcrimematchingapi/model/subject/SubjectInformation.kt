package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaSubjectInformationDTO

data class SubjectInformation(
  val nomisId: String,
  val name: String?,
  val dateOfBirth: String?,
  val address: String?,
  val orderStartDate: String?,
  val orderEndDate: String?,
  val deviceId: String?,
  val tagPeriodStartDate: String?,
  val tagPeriodEndDate: String?
) {
  constructor(dto: AthenaSubjectInformationDTO) : this(
    nomisId = dto.nomisId,
    name = dto.name,
    dateOfBirth = dto.dateOfBirth,
    address = dto.address,
    orderStartDate = dto.orderStartDate,
    orderEndDate = dto.orderEndDate,
    deviceId = dto.deviceId,
    tagPeriodStartDate = dto.tagPeriodStartDate,
    tagPeriodEndDate = dto.tagPeriodEndDate
  )
}
