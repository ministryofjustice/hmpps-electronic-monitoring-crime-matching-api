package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class CrimeResponse(
  val id: String,
  val crimeTypeId: String,
  val policeForce: String,
  val crimeTypeDescription: String,
  val crimeReference: String,
  val crimeDateTimeFrom: String,
  val crimeDateTimeTo: String,
  val latitude: Double,
  val longitude: Double,
  val crimeText: String,
)
