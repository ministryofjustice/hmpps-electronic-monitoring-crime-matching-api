package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class CrimeMatchingResultDto(
  @field:NotNull(message = "crimeVersionId is required")
  val crimeVersionId: UUID,

  @field:Valid
  val deviceWearers: List<CrimeMatchingResultDeviceWearerDto>,
)
