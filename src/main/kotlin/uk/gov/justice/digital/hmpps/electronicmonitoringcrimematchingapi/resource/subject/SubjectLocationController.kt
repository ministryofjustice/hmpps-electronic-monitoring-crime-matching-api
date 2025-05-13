package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.subject

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Pattern
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.crimemapping.CrimeBatchSearchCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectInformation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectLocation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectLocationSearchCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.SubjectService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.AuditService

@RestController
@PreAuthorize("hasRole('ROLE_EM_CRIME_MATCHING_GENERAL_RO')")
@RequestMapping("/subjects/locations", produces = ["application/json"])
class SubjectLocationController(
) {

  @Operation(
    tags = ["Subject Locations"],
    summary = "Get the location data for the request subject IDs and date parameters",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getSubjectLocations(
    authentication: Authentication,
    @RequestBody subjectLocationSearchCriteria: SubjectLocationSearchCriteria,
  ): ResponseEntity<List<SubjectLocation>> {
    TODO()
  }
}