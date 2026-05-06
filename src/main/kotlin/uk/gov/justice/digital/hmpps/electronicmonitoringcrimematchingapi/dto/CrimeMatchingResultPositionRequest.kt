package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CrimeMatchingResultPositionRequest(
  @field:NotNull(message = "capturedDateTime is required")
  val capturedDateTime: LocalDateTime,

  @field:NotNull(message = "direction is required")
  val direction: Long,

  @field:NotNull(message = "latitude is required")
  val latitude: Double,

  @field:NotNull(message = "longitude is required")
  val longitude: Double,

  @field:NotNull(message = "precision is required")
  val precision: Long,

  @field:NotBlank(message = "sequenceLabel is required")
  val sequenceLabel: String,

  @field:NotNull(message = "speed is required")
  val speed: Long,
)
