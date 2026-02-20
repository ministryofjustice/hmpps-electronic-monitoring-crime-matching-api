package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchIngestionAttemptSummaryResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PagedResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchService
import java.time.LocalDateTime

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO')")
@RequestMapping("/ingestion-attempts", produces = ["application/json"])
class CrimeBatchIngestionAttemptController(
  val crimeBatchService: CrimeBatchService,
) {
  @Operation(
    tags = ["Crime Batch Ingestion Attempt"],
    summary = "Get crime batch ingestion attempt summaries",
  )
  @GetMapping(
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getCrimeBatchIngestionAttemptSummaries(
    @Parameter(
      description = "Filters results to the specified batch ID. If omitted, results for all batch IDs are returned.",
      required = false,
    )
    @RequestParam(required = false)
    batchId: String? = null,
    @Parameter(
      description = "Filters results to the specified police force area. If omitted, results for all areas are returned",
      required = false,
    )
    @RequestParam(required = false)
    policeForceArea: String? = null,
    @Parameter(
      description = "Inclusive start of the batch creation timestamp filter (ISO-8601). If omitted, no lower bound is applied.",

      required = false,
      example = "2025-01-01T00:00:00",
    )
    @RequestParam(required = false)
    fromDate: LocalDateTime? = null,
    @Parameter(
      description = "Inclusive end of the batch creation timestamp filter (ISO-8601). If omitted, no upper bound is applied.",
      required = false,
      example = "2025-01-31T23:59:59",
    )
    @RequestParam(required = false)
    toDate: LocalDateTime? = null,
    @Parameter(
      description = "Page number (0-based). Defaults to 0.",
      example = "0",
    )
    @RequestParam(defaultValue = "0")
    page: Int = 0,
    @Parameter(
      description = "Number of items per page",
      example = "30",
    )
    @RequestParam(defaultValue = "30")
    pageSize: Int = 30,
  ): ResponseEntity<PagedResponse<CrimeBatchIngestionAttemptSummaryResponse>> {
    val results = crimeBatchService.getCrimeBatchIngestionAttemptSummaries(
      batchId,
      policeForceArea,
      fromDate,
      toDate,
      page,
      pageSize,
    )

    return ResponseEntity.status(200).body(
      PagedResponse(
        listOf(),
      ),
    )
  }
}
