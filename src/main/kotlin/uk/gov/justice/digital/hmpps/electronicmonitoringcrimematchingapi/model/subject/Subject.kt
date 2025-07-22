package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.nullableLocalDateTime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaSubjectDTO
import java.time.LocalDateTime

data class Subject(
  val personId: String,
  val nomisId: String,
  val name: String?,
  val dateOfBirth: String?,
  val address: String?,
  val orderStartDate: LocalDateTime?,
  val orderEndDate: LocalDateTime?,
  val deviceId: String?,
  val tagPeriodStartDate: LocalDateTime?,
  val tagPeriodEndDate: LocalDateTime?,
) {
  constructor(dto: AthenaSubjectDTO) : this(
    personId = dto.personId,
    nomisId = dto.nomisId,
    name = dto.name,
    dateOfBirth = dto.dateOfBirth,
    address = dto.address,
    orderStartDate = nullableLocalDateTime(dto.orderStartDate),
    orderEndDate = nullableLocalDateTime(dto.orderEndDate),
    deviceId = dto.deviceId,
    tagPeriodStartDate = nullableLocalDateTime(dto.tagStartDate),
    tagPeriodEndDate = nullableLocalDateTime(dto.tagEndDate),
  )
}
