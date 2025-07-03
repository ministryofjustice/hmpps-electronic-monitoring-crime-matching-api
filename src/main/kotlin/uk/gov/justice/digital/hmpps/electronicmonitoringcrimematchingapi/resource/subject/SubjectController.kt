package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.subject

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.Subject
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.subject.SubjectService

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING_GENERAL_RO')")
@RequestMapping("/subjects", produces = ["application/json"])
class SubjectController(
  @Autowired val subjectService: SubjectService,
) {

  @Operation(
    tags = ["Subjects"],
    summary = "Search for subjects",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getSubjects(
    authentication: Authentication,
    @Parameter(description = "The search criteria for the query", required = true)
    @ModelAttribute subjectsQueryCriteria: SubjectsQueryCriteria,
  ): ResponseEntity<List<Subject>> {
    val result = subjectService.getSubjectsQueryResults(subjectsQueryCriteria, authentication.name)
    return ResponseEntity.ok(result)
  }
}
