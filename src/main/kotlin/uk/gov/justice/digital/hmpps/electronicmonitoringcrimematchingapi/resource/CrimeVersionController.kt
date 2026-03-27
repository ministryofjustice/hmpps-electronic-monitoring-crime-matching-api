package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeVersionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeVersionSummaryResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PagedResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.Response
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers.CrimeVersionMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers.CrimeVersionSummaryMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.proximityAlert.CrimeVersionService
import java.util.UUID

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING_GENERAL_RO')")
@RequestMapping("/crime-versions", produces = ["application/json"])
class CrimeVersionController(
  private val crimeVersionService: CrimeVersionService,
  private val crimeVersionSummaryMapper: CrimeVersionSummaryMapper,
  private val crimeVersionMapper: CrimeVersionMapper,
) {

  @Operation(
    tags = ["Crime Versions"],
    summary = "Search crime versions by crime reference",
  )
  @GetMapping
  fun searchCrimeVersions(
    @Parameter(
      description = "Crime reference prefix to search for",
      required = true,
    )
    @RequestParam
    crimeRef: String,
    @Parameter(
      description = "Page number (0-based)",
      example = "0",
    )
    @RequestParam(defaultValue = "0")
    page: Int,
    @Parameter(
      description = "Number of items per page",
      example = "30",
    )
    @RequestParam(defaultValue = "30")
    pageSize: Int,
  ): ResponseEntity<PagedResponse<CrimeVersionSummaryResponse>> {
    if (crimeRef.isBlank()) {
      throw ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "crimeRef must not be blank",
      )
    }

    val results = crimeVersionService.searchCrimeVersions(
      crimeRef,
      page,
      pageSize,
    )

    return ResponseEntity.ok(
      PagedResponse(
        results.content.map { crimeVersionSummaryMapper.toDto(it) },
        results.totalPages,
        results.number,
        results.size,
      ),
    )
  }

  @Operation(
    tags = ["Crime Version"],
    summary = "Get crime version",
  )
  @GetMapping(
    path = [
      "/{crimeVersionId}",
    ],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getCrimeVersion(
    @Parameter(
      description = "The ID of the crime version",
      required = true,
      example = "aefa6893-2bed-4e69-a69e-afb562046a6f",
    )
    @PathVariable crimeVersionId: UUID,
  ): ResponseEntity<Response<CrimeVersionResponse>> {
    val result = crimeVersionService.getCrimeVersion(crimeVersionId)

    return ResponseEntity.ok(
      Response(crimeVersionMapper.toDto(result)),
    )
  }
}
