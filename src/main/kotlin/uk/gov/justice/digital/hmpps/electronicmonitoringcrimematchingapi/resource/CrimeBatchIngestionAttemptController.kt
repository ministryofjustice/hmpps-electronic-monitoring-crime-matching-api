package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PagedResponseDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.IngestionAttemptSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchEmailIngestionService
import java.time.LocalDateTime

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING__CRIME_BATCH_INGESTION_ATTEMPTS__RO')")
@RequestMapping("/ingestion-attempts", produces = ["application/json"])
class CrimeBatchIngestionAttemptController(
  private val crimeBatchEmailIngestionService: CrimeBatchEmailIngestionService,
) {
  @Operation(
    tags = ["Crime Batch Ingestion Attempt"],
    summary = "Get a list of crime batch ingestion attempts",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getCrimeBatch(
    batchId: String = "",
    policeForceArea: String? = null,
    fromDate: LocalDateTime? = LocalDateTime.now(),
    toDate: LocalDateTime? = LocalDateTime.now(),
    page: Int = 0,
    pageSize: Int = 30,
  ): ResponseEntity<PagedResponseDto<IngestionAttemptSummary>> {
    // Build this elsewhere?
    val pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"))
    val res = this.crimeBatchEmailIngestionService.getCrimeBatchIngestionAttempts(
      batchId,
      policeForceArea,
      fromDate,
      toDate,
      pageable,
    )
    return ResponseEntity.ok(PagedResponseDto(res.content, res.totalPages, page, res.size))
  }
}
