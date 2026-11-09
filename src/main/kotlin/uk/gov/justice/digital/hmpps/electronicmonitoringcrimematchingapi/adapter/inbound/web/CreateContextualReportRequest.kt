package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.adapter.inbound.web

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class CreateContextualReportRequest(
  @field:NotNull(message = "startTime is required")
  val startTime: Instant,

  @field:NotNull(message = "endTime is required")
  val endTime: Instant,

  @field:NotBlank(message = "policeForceArea is required")
  val policeForceArea: String,

  @field:NotBlank(message = "officerName is required")
  val officerName: String,

  @field:NotNull(message = "hubManagerId is required")
  val hubManagerId: UUID,

  @field:NotNull(message = "requestedAt is required")
  val requestedAt: Instant,

  val additionalInformation: String,
) {
  @get:AssertTrue(message = "endTime must be after startTime")
  @get:JsonIgnore
  val hasValidTimeRange: Boolean
    get() = endTime.isAfter(startTime)

  @get:AssertTrue(message = "The report time window must not exceed 12 hours")
  @get:JsonIgnore
  val hasValidTimeWindow: Boolean
    get() = Duration.between(startTime, endTime) <= Duration.ofHours(12)
}
