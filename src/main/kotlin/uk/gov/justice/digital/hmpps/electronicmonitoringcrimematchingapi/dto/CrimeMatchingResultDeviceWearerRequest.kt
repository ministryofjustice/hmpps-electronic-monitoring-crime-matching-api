package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CrimeMatchingResultDeviceWearerRequest(
  @field:NotBlank(message = "Address is required")
  val address: String,

  @field:NotNull(message = "Date of birth is required")
  val dateOfBirth: LocalDateTime,

  @field:NotNull(message = "deviceId is required")
  val deviceId: Long,

  @field:NotBlank(message = "deviceName is required")
  val deviceName: String,

  @field:NotBlank(message = "identifier is required")
  val identifier: String,

  @field:NotBlank(message = "name is required")
  val name: String,

  @field:NotBlank(message = "nomisId is required")
  val nomisId: String,

  @field:NotBlank(message = "pncRef is required")
  val pncRef: String,

  @field:Valid
  val positions: List<CrimeMatchingResultPositionRequest>,
)
