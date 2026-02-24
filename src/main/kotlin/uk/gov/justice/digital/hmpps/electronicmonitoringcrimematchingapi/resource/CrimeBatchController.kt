package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.Response
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers.CrimeBatchMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchService

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO')")
@RequestMapping("/crime-batches", produces = ["application/json"])
class CrimeBatchController(
  private val crimeBatchService: CrimeBatchService,
  private val crimeBatchMapper: CrimeBatchMapper,
) {
  @Operation(
    tags = ["Crime Batch"],
    summary = "Get a crime batch",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    path = [
      "/{crimeBatchId}",
    ],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getCrimeBatch(
    @PathVariable crimeBatchId: String,
  ): ResponseEntity<Response<CrimeBatchResponse>> {
    val batch = this.crimeBatchService.getCrimeBatch(crimeBatchId)

    return ResponseEntity.ok(
      Response(
        crimeBatchMapper.toDto(batch),
      ),
    )
  }
}
