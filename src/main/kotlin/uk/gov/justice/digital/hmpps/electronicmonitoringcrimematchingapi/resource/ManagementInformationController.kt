package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeSubmissionSummaryResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.FailedCrimeSubmissionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PagedResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.ZeroPrecisionCrimeMatchResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers.FailedCrimeSubmissionMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers.ZeroPrecisionCrimeMatchMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.ManagementInformationService
import java.time.Instant

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING__MI__RO')")
@RequestMapping("/management-information", produces = ["application/json"])
class ManagementInformationController(
  private val managementInformationService: ManagementInformationService,
  private val zeroPrecisionCrimeMatchMapper: ZeroPrecisionCrimeMatchMapper,
  private val failedCrimeSubmissionMapper: FailedCrimeSubmissionMapper,
) {
  @Operation(
    tags = ["Management Information"],
    summary = "Get zero precision crime matches",
  )
  @GetMapping("/zero-precision-crime-matches")
  fun getZeroPrecisionCrimeMatches(
    @RequestParam(value = "from", required = false)
    from: Instant? = null,
    @RequestParam(value = "to", required = false)
    to: Instant? = null,
    @RequestParam(value = "page", required = false)
    page: Int? = null,
  ): ResponseEntity<PagedResponse<ZeroPrecisionCrimeMatchResponse>> {
    val results = managementInformationService.getZeroPrecisionCrimeMatches(
      from = from,
      to = to,
      page = page,
      pageSize = 10,
    )

    return ResponseEntity.status(200).body(
      PagedResponse(
        results.content.map { zeroPrecisionCrimeMatchMapper.toDto(it) },
        results.totalPages,
        results.number,
        results.size,
      ),
    )
  }

  @Operation(
    tags = ["Management Information"],
    summary = "Get failed crime submissions",
  )
  @GetMapping("/failed-crime-submissions")
  fun getFailedCrimeSubmissions(
    @RequestParam(value = "policeForceArea", required = false)
    policeForceArea: PoliceForce? = null,
    @RequestParam(value = "from", required = false)
    from: Instant? = null,
    @RequestParam(value = "to", required = false)
    to: Instant? = null,
  ): ResponseEntity<PagedResponse<FailedCrimeSubmissionResponse>> {
    val results = managementInformationService.getFailedCrimeSubmissions(
      policeForceArea = policeForceArea,
      from = from,
      to = to,
    )

    return ResponseEntity.ok().body(
      PagedResponse(
        listOf(failedCrimeSubmissionMapper.toDto(results)),
        pageCount = 1,
        pageNumber = 0,
        pageSize = results.size,
      ),
    )
  }

  @Operation(
    tags = ["Management Information"],
    summary = "Get crimes submission summary",
  )
  @GetMapping("/crimes-submission-summary")
  fun getCrimeSubmissionSummary(
    @RequestParam(value = "from", required = false)
    from: Instant? = null,
    @RequestParam(value = "to", required = false)
    to: Instant? = null,
    @RequestParam(value = "policeForceArea", required = false)
    policeForceArea: String? = null,
  ): ResponseEntity<PagedResponse<CrimeSubmissionSummaryResponse>> {
    val results = managementInformationService.getCrimeSubmissionSummary(
      from = from,
      to = to,
      policeForceArea = policeForceArea,
    )

    return ResponseEntity.status(200).body(
      PagedResponse(
        results,
        pageCount = 1,
        pageNumber = 0,
        pageSize = results.size,
      ),
    )
  }
}
