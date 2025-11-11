package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.ValidationErrors.Crime.EASTING_MAX
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.ValidationErrors.Crime.EASTING_MIN
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.ValidationErrors.Crime.INVALID_CRIME_DATE
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.ValidationErrors.Crime.INVALID_CRIME_REFERENCE
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.ValidationErrors.Crime.INVALID_LOCATION_DATA
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.ValidationErrors.Crime.LATITUDE_MAX
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.ValidationErrors.Crime.LATITUDE_MIN
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.ValidationErrors.Crime.LONGITUDE_MAX
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.ValidationErrors.Crime.LONGITUDE_MIN
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.ValidationErrors.Crime.NORTHING_MAX
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.ValidationErrors.Crime.NORTHING_MIN
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.GeodeticDatum
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import java.time.LocalDateTime

data class CrimeRecordDto(
  val policeForce: PoliceForce,

  val crimeTypeId: CrimeType,

  @field:NotBlank(message = INVALID_CRIME_REFERENCE)
  val crimeReference: String,

  val crimeDateTimeFrom: LocalDateTime,

  val crimeDateTimeTo: LocalDateTime,

  @field:DecimalMin(value = "0", message = EASTING_MIN)
  @field:DecimalMax(value = "600000", message = EASTING_MAX)
  val easting: Double?,

  @field:DecimalMin(value = "0", message = NORTHING_MIN)
  @field:DecimalMax(value = "1300000", message = NORTHING_MAX)
  val northing: Double?,

  @field:DecimalMin(value = "49.5", message = LATITUDE_MIN)
  @field:DecimalMax(value = "61.5", message = LATITUDE_MAX)
  val latitude: Double?,

  @field:DecimalMin(value = "-8.5", message = LONGITUDE_MIN)
  @field:DecimalMax(value = "2.6", message = LONGITUDE_MAX)
  val longitude: Double?,

  val datum: GeodeticDatum,

  val crimeText: String,
) {
  @AssertTrue(message = INVALID_CRIME_DATE)
  fun isValidCrimeDateRange(): Boolean = try {
    crimeDateTimeFrom.isBefore(crimeDateTimeTo) || crimeDateTimeFrom.isEqual(crimeDateTimeTo)
  } catch (e: Exception) {
    false
  }

  @AssertTrue(message = INVALID_LOCATION_DATA)
  fun isLocationDataValid(): Boolean {
    val hasGridRef = easting != null && northing != null
    val hasLatLong = latitude != null && longitude != null

    return hasGridRef xor hasLatLong
  }
}
