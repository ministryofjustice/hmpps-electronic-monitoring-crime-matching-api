package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource

import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CreateCrimeMatchingRunRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CreateCrimeMatchingRunResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.Response
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeMatching.CrimeMatchingRunService

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING__CRIME_MATCHING_RESULTS__RW')")
@RequestMapping("/crime-matching-run", produces = ["application/json"])
class CrimeMatchingRunController(
  private val crimeMatchingRunService: CrimeMatchingRunService,
) {
  @Operation(
    tags = ["Crime Matching"],
    summary = "Create a crime matching run",
  )
  @PostMapping(
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun createCrimeMatchingRun(
    @Valid @RequestBody runDto: CreateCrimeMatchingRunRequest,
  ): ResponseEntity<Response<CreateCrimeMatchingRunResponse>> {
    val run = crimeMatchingRunService.createCrimeMatchingRun(runDto)

    return ResponseEntity.status(201).body(
      Response(
        CreateCrimeMatchingRunResponse(
          id = run.id.toString(),
        ),
      ),
    )
  }
}
