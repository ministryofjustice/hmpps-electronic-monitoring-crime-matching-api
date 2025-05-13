package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.crimemapping

import org.joda.time.DateTime
import java.util.Date

//Used to show a mapped crime per mapped subject
data class MappedCrime(
  val crimeRef: String,
  val crimeType: String,
  val batchId: String,
  val policeForce: String,
  val fromDateTime: DateTime,
  val toDateTime: DateTime,
  val crimeLongitude: Double,
  val crimeLatitude: Double,
  val otherInfo: String,
  val deviceId: String,
  val deviceName: String,
  val subjectId: String,
  val offenderName: String,
  val nomisId: String,
  val pncRef: String,
  val subjectAddress: String,
  val subjectDateOfBirth: Date,
  val subjectManager: String,
  val subjectLocationDateTime: DateTime,
  val subjectLongitude: Double,
  val subjectLatitude: Double,
  val confidence: Double,
  val height: Double,
  val speed: Double,
  val direction: Double,
  val sequenceNum: Int
)