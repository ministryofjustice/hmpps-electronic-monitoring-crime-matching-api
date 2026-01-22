package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeMatchingStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeMatchingTriggerType
import java.time.LocalDateTime
import java.util.UUID

data class CrimeMatchingRunDto(
  @field:NotNull(message = "crimeBatchId is required")
  val crimeBatchId: UUID,

  @field:NotBlank(message = "algorithmVersion is required")
  val algorithmVersion: String,

  @field:NotNull(message = "triggerType is required")
  val triggerType: CrimeMatchingTriggerType,

  @field:NotNull(message = "status is required")
  val status: CrimeMatchingStatus,

  @field:NotNull(message = "matchingStarted is required")
  val matchingStarted: LocalDateTime,

  @field:NotNull(message = "matchingEnded is required")
  val matchingEnded: LocalDateTime,

  @field:Valid
  val results: List<CrimeMatchingResultDto>,
)
