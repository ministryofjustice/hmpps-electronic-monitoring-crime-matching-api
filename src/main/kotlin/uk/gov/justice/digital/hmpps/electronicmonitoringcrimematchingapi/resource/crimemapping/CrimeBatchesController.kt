package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.crimemapping

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.crimemapping.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.crimemapping.CrimeBatchSearchCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.crimemapping.MappedCrime

@RestController
@PreAuthorize("hasRole('ROLE_EM_CRIME_MATCHING_GENERAL_RO')")
@RequestMapping("/crime-mapping", produces = ["application/json"])
class CrimeBatchesController() {

  @Operation(
    tags = ["Crime Mapping"],
    summary = "Get the list of crime batches",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    path = [
      "/batches",
    ],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getCrimeBatches(
      authentication: Authentication,
      @RequestBody crimeBatchSearchCriteria: CrimeBatchSearchCriteria,
  ): ResponseEntity<List<CrimeBatch>> {
    TODO()
  }

  @Operation(
    tags = ["Crime Mapping"],
    summary = "Get the mapped crime batch details",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    path = [
      "/batches/{batchId}",
    ],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getCrimeBatch(
      authentication: Authentication,
      @Parameter(description = "The legacy subject ID", required = true)
    @PathVariable batchId: String,
  ): ResponseEntity<List<MappedCrime>> {
    TODO()
    //Currently a list of mapped crimes but an alternate is something like a mappedcrimeresults object that then contains mapped subjects for example?
  }
}