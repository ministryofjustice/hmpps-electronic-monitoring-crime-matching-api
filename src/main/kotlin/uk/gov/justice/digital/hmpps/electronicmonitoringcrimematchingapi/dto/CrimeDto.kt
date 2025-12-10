package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import java.util.UUID

data class CrimeDto(
  val id: UUID,
  val crimeTypeId: String,
  val crimeTypeDescription: String,
  val crimeReference: String,
  val crimeDateTimeFrom: String,
  val crimeDateTimeTo: String,
  val latitude: Double,
  val longitude: Double,
  val crimeText: String,
) {
  constructor(crime: CrimeVersion) : this(
    id = crime.id,
    crimeTypeId = crime.crimeTypeId.name,
    crimeTypeDescription = crime.crimeTypeId.value,
    crimeReference = crime.crime.crimeReference,
    crimeDateTimeFrom = crime.crimeDateTimeFrom.toString(),
    crimeDateTimeTo = crime.crimeDateTimeTo.toString(),
    latitude = crime.latitude ?: 0.0,
    longitude = crime.longitude ?: 0.0,
    crimeText = crime.crimeText,
  )
}
