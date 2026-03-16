package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import java.time.Instant

data class CrimeRecordRequest(
  val policeForce: PoliceForce,

  val crimeTypeId: CrimeType,

  val batchId: String,

  val crimeReference: String,

  val crimeDateTimeFrom: Instant,

  val crimeDateTimeTo: Instant,

  val easting: Double?,

  val northing: Double?,

  val latitude: Double?,

  val longitude: Double?,

  val crimeText: String,
)
