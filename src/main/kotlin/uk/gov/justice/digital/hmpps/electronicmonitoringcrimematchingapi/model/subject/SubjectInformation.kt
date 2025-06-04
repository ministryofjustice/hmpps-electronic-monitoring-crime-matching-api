package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.nullableLocalDateTime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaSubjectInformationDTO
import java.time.LocalDateTime

data class SubjectInformation(
  val nomisId: String,
  val name: String?,
  val dateOfBirth: LocalDateTime?,
  val address: String?,
  val orderStartDate: LocalDateTime?,
  val orderEndDate: LocalDateTime?,
  val deviceId: String?,
  val tagPeriodStartDate: LocalDateTime?,
  val tagPeriodEndDate: LocalDateTime?,
) {
  constructor(dto: AthenaSubjectInformationDTO) : this(
    nomisId = dto.nomisId,
    name = dto.name,
    dateOfBirth = nullableLocalDateTime(dto.dateOfBirth),
    address = dto.address,
    orderStartDate = nullableLocalDateTime(dto.orderStartDate),
    orderEndDate = nullableLocalDateTime(dto.orderEndDate),
    deviceId = dto.deviceId,
    tagPeriodStartDate = nullableLocalDateTime(dto.tagPeriodStartDate),
    tagPeriodEndDate = nullableLocalDateTime(dto.tagPeriodEndDate),
  )
}
