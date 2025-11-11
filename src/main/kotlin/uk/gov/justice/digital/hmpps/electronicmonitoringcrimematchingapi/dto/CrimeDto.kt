package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime

data class CrimeDto(
  val id: Long,
  val crimeTypeId: String,
  val crimeTypeDescription: String,
  val crimeReference: String,
  val crimeDateTimeFrom: String,
  val crimeDateTimeTo: String,
  val latitude: Double,
  val longitude: Double,
  val crimeText: String,
) {
  constructor(crime: Crime) : this(
    id = crime.id!!,
    crimeTypeId = crime.crimeTypeId.name,
    crimeTypeDescription = crime.crimeTypeId.value,
    crimeReference = crime.crimeReference,
    crimeDateTimeFrom = crime.crimeDateTimeFrom.toString(),
    crimeDateTimeTo = crime.crimeDateTimeTo.toString(),
    latitude = crime.latitude ?: 0.0,
    longitude = crime.longitude ?: 0.0,
    crimeText = crime.crimeText,
  )
}
