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
  constructor(version: CrimeVersion) : this(
    id = version.id.toString(),
    crimeTypeId = version.crimeTypeId.name,
    crimeTypeDescription = version.crimeTypeId.value,
    policeForce = version.crime.policeForceArea.name,
    crimeReference = version.crime.crimeReference,
    crimeDateTimeFrom = version.crimeDateTimeFrom.toString(),
    crimeDateTimeTo = version.crimeDateTimeTo.toString(),
    latitude = version.latitude ?: 0.0,
    longitude = version.longitude ?: 0.0,
    crimeText = version.crimeText,
  )
}
