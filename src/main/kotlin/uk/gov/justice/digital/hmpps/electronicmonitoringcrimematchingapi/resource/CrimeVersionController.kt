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
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeVersionSummaryResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PagedResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers.CrimeVersionSummaryMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.proximityAlert.CrimeVersionService

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING_GENERAL_RO')")
@RequestMapping("/crime-versions", produces = ["application/json"])
class CrimeVersionController(
  private val crimeVersionService: CrimeVersionService,
  private val crimeVersionSummaryMapper: CrimeVersionSummaryMapper,
) {

  @Operation(
    tags = ["Crime Versions"],
    summary = "Search crime versions by crime reference",
  )
  @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
  fun searchCrimeVersions(
    @Parameter(
      description = "Crime reference prefix to search for",
      required = true,
      example = "ABC123",
    )
    @RequestParam
    crimeRef: String,
    @Parameter(
      description = "Page number (0-based)",
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
  ): ResponseEntity<PagedResponse<CrimeVersionSummaryResponse>> {
    val results = crimeVersionService.searchCrimeVersions(
      crimeRef,
      page,
      pageSize,
    )

    return ResponseEntity.status(200).body(
      PagedResponse(
        results.content.map { crimeVersionSummaryMapper.toDto(it) },
        results.totalPages,
        results.number,
        results.size,
      ),
    )
  }
}
