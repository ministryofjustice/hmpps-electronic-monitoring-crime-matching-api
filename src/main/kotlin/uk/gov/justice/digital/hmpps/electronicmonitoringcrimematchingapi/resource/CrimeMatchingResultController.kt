package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeMatchingResultResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.Response
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.CrimeMatchingResultsService

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING__CRIME_MATCHING_RESULTS__RO')")
@RequestMapping("/crimes", produces = ["application/json"])
class CrimeMatchingResultController(
  val crimeMatchingResultsService: CrimeMatchingResultsService,
) {
  @Operation(
    tags = ["Crime Matching Results"],
    summary = "Get crime matching results",
  )
  @GetMapping(
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getCrimes(
    @RequestParam("batchId") batchIds: List<String>,
  ): ResponseEntity<Response<List<CrimeMatchingResultResponse>>> {
    val results = crimeMatchingResultsService.getCrimesMatchingResultsForBatches(batchIds)

    return ResponseEntity.status(201).body(
      Response(
        listOf()
      ),
    )
  }
}
