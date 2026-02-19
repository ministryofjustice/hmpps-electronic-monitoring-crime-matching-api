package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class CrimeMatchingResultResponse(
  val policeForce: String,
  val batchId: String,
  val crimeRef: String,
  val crimeType: String,
  val crimeDateTimeFrom: String,
  val crimeDateTimeTo: String,
  val crimeLatitude: Double,
  val crimeLongitude: Double,
  val crimeText: String,
  val deviceId: String,
  val deviceName: String,
  val subjectId: String,
  val subjectName: String,
  val subjectNomisId: String,
  val subjectPncRef: String,
  val subjectAddress: String,
  val subjectDateOfBirth: String,
  val subjectManager: String,
)
