package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CrimeMatchingResultPositionRequest(
  @field:NotNull(message = "latitude is required")
  val latitude: Double,

  @field:NotNull(message = "longitude is required")
  val longitude: Double,

  @field:NotNull(message = "capturedDateTime is required")
  val capturedDateTime: LocalDateTime,

  @field:NotBlank(message = "sequenceLabel is required")
  val sequenceLabel: String,

  @field:NotNull(message = "confidenceCircle is required")
  val confidenceCircle: Int,
)
