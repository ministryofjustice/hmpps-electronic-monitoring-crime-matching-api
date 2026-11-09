package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.adapter.inbound.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.adapter.inbound.web.CreateContextualReportRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.application.usecase.CreateContextualReport
import java.util.UUID

@RestController
@Tag(
  name = "Contextual reports",
  description = "Create and retrieve contextual reports",
)
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING__DOCUMENT_REQUESTS__RW')")
@RequestMapping("/crime-matching-results/{crimeMatchingResultId}/matched-wearers/{deviceId}/contextual-reports")
class ContextualReportController(
  private val createContextualReport: CreateContextualReport,
) {
  @PostMapping
  @Operation(
    summary = "Create a contextual report",
    description = "Create a request to generate a contextual report.",
  )
  @ApiResponse(
    responseCode = "202",
    description = "Contextual report request accepted",
  )
  @ApiResponse(
    responseCode = "400",
    description = "Invalid contextual report request",
    content = [Content()],
  )
  fun createContextualReportRequest(
    @PathVariable crimeMatchingResultId: UUID,
    @PathVariable deviceId: Long,
    @Valid @RequestBody request: CreateContextualReportRequest,
    @Parameter(hidden = true)
    authentication: Authentication,
  ): ResponseEntity<String> {
    val id = createContextualReport.create(crimeMatchingResultId.toString(), deviceId, request, authentication.name)
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(id)
  }
}
