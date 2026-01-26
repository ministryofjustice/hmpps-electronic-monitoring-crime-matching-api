package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CrimeMatchingResultDeviceWearerDto(
  @field:NotNull(message = "deviceId is required")
  val deviceId: Long,

  @field:NotBlank(message = "name is required")
  val name: String,

  @field:NotBlank(message = "nomisId is required")
  val nomisId: String,

  @field:Valid
  val positions: List<CrimeMatchingResultPositionDto>,
)
