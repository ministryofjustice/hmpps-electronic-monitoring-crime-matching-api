package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject

import org.joda.time.DateTime
import java.util.Date

data class SubjectInformation(
    var legacySubjectId: String,
    var name: String?,
    var address: String?,
    var dateOfBirth: Date?,
    var deviceId: String,
    var startDate: Date,
    var endDate: Date?,
    var nomisId: String?,
    var tagPeriodStart: DateTime,
    var tagPeriodEnd: DateTime?
)