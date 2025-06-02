package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.subject

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.PageResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.QueryExecutionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectInformation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectSearchCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.SubjectService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.AuditService

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING_GENERAL_RO')")
@RequestMapping("/subjects", produces = ["application/json"])
class SubjectController(
  @Autowired val subjectService: SubjectService,
  @Autowired val auditService: AuditService,
) {

  @Operation(
    tags = ["Subjects"],
    summary = "Execute a search for subjects",
  )
  @RequestMapping(
    method = [RequestMethod.POST],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun executeSearch(
    authentication: Authentication,
    @Parameter(description = "The search criteria for the query", required = true)
    @RequestBody subjectSearchCriteria: SubjectSearchCriteria,
  ): ResponseEntity<QueryExecutionResponse> {
    val queryExecutionId = subjectService.getQueryExecutionId(subjectSearchCriteria)

    auditService.createEvent(
      authentication.name,
      "SUBJECT_SEARCH",
      mapOf(
        "queryExecutionId" to queryExecutionId,
      ),
    )

    return ResponseEntity.ok(QueryExecutionResponse(queryExecutionId))
  }

  @Operation(
    tags = ["Subjects"],
    summary = "Retrieve search results for subjects using query execution id",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getSubjectSearchResults(
    @Parameter(description = "The query execution id of search", required = true)
    @RequestParam(name = "id") queryExecutionId: String,
    @Parameter(description = "The requested page number", required = true)
    @RequestParam(name = "page")
    page: Int = 1,
    @Parameter(description = "The requested page size", required = true)
    @RequestParam(name = "pageSize")
    pageSize: Int = 1
  ): ResponseEntity<PageResult<SubjectInformation>> {
    val result = subjectService.getSubjectSearchResults(queryExecutionId, page, pageSize)
    return ResponseEntity.ok(result)
  }
}
