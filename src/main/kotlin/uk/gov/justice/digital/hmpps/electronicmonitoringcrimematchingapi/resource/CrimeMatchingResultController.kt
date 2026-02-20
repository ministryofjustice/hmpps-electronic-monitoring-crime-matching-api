package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.constraints.NotEmpty
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeMatchingResultResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.Response
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers.CrimeMatchingResultMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.CrimeMatchingResultsService

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING__CRIME_MATCHING_RESULTS__RO')")
@RequestMapping("/crime-matching-results", produces = ["application/json"])
class CrimeMatchingResultController(
  val crimeMatchingResultsService: CrimeMatchingResultsService,
  val crimeMatchingResultMapper: CrimeMatchingResultMapper,
) {
  @Operation(
    tags = ["Crime Matching Results"],
    summary = "Get crime matching results",
  )
  @GetMapping(
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getCrimeMatchingResults(
    @RequestParam("batchId")
    @NotEmpty(message = "At least one batchId must be provided")
    batchIds: List<String> = listOf(),
  ): ResponseEntity<Response<List<CrimeMatchingResultResponse>>> {
    val results = crimeMatchingResultsService.getCrimesMatchingResultsForBatches(batchIds)

    return ResponseEntity.status(200).body(
      Response(
        results.map { crimeMatchingResultMapper.toDto(it) },
      ),
    )
  }
}
