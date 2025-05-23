package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject

import org.joda.time.DateTime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.AthenaSubjectInformationDTO
import java.util.Date

data class SubjectInformation(
  //TODO correct datatypes
  var nomisId: String,
  var name: String?,
  var dateOfBirth: String?,
  var address: String?,
  var orderStartDate: String?,
  var orderEndDate: String?,
  var deviceId: String?,
  var tagPeriodStartDate: String?,
  var tagPeriodEndDate: String?
) {
  //TODO correct namings need to be set for actual fields in datastore
  //TODO Set vars to vals where possible
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
