package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion

data class CrimeDto(
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
) {
  constructor(crime: CrimeVersion) : this(
    id = crime.id.toString(),
    crimeTypeId = crime.crimeTypeId.name,
    crimeTypeDescription = crime.crimeTypeId.value,
    policeForce = crime.crime.policeForceArea.name,
    crimeReference = crime.crime.crimeReference,
    crimeDateTimeFrom = crime.crimeDateTimeFrom.toString(),
    crimeDateTimeTo = crime.crimeDateTimeTo.toString(),
    latitude = crime.latitude ?: 0.0,
    longitude = crime.longitude ?: 0.0,
    crimeText = crime.crimeText,
  )
}
