package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.ValidationErrors.Crime.INVALID_BATCH_ID
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.ValidationErrors.Crime.INVALID_CRIME_REFERENCE
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import java.time.LocalDateTime

data class CrimeRecordDto(
  val policeForce: PoliceForce,

  val crimeTypeId: CrimeType,

  @field:NotBlank(message = INVALID_BATCH_ID)
  val batchId: String,

  @field:NotBlank(message = INVALID_CRIME_REFERENCE)
  val crimeReference: String,

  val crimeDateTimeFrom: LocalDateTime,

  val crimeDateTimeTo: LocalDateTime,

  val easting: Double?,

  val northing: Double?,

  val latitude: Double?,

  val longitude: Double?,

  val crimeText: String,
)
