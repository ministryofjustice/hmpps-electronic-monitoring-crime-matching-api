package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject

import org.joda.time.DateTime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.AthenaSubjectInformationDTO
import java.util.Date

data class SubjectInformation(
  var subjectId: String,
  var name: String?,
//  var address: String?,
//  var dateOfBirth: Date?,
//  var deviceId: String,
//  var orderStartDate: Date,
//  var orderEndDate: Date?,
//  var nomisId: String?,
//  var tagPeriodStart: DateTime,
//  var tagPeriodEnd: DateTime?
) {
  constructor(dto: AthenaSubjectInformationDTO) : this(
    subjectId = dto.legacySubjectId,
    name = dto.name
  )
}
