package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject

import org.joda.time.DateTime
import java.util.Date

data class SubjectLocation(
    var subjectId: String,
    var confidence: String,
    var height: String,
    var speed: String,
    var direction: String,
    var sequenceNumber: String,
    var geolocationMechanismId: String,
    var geolocationMechanism: String,
    var capturedDateTime: DateTime,
    var longitude: Double,
    var latitude: Double
  //Should we also include subject details here (Address etc) - similar to existing tool
)