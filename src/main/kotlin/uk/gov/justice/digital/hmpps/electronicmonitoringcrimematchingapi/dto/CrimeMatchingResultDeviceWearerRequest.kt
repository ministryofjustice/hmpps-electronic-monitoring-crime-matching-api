package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CrimeMatchingResultDeviceWearerRequest(
  val address: String,

  val dateOfBirth: LocalDateTime?,

  @field:NotNull(message = "deviceId is required")
  val deviceId: Long,

  val deviceSerialNumber: String,

  val deviceName: String,

  val identifier: String,

  @field:NotBlank(message = "name is required")
  val name: String,

  val nomisId: String,

  val pncRef: String,

  @field:Valid
  val positions: List<CrimeMatchingResultPositionRequest>,
)
