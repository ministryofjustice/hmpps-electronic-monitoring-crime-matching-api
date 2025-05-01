package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.Pattern
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SubjectInformation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.AthenaRoleService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.SubjectService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.AuditService

@RestController
@PreAuthorize("hasRole('ROLE_EM_CRIME_MATCHING_GENERAL_RO', 'ROLE_TEMPLATE_KOTLIN__UI')")
@RequestMapping("/subjects", produces = ["application/json"])
class SubjectController (
  @Autowired val subjectService: SubjectService,
  val athenaRoleService: AthenaRoleService,
  @Autowired val auditService: AuditService,
) {

  @Operation(
  tags = ["Subjects"],
  summary = "Get the summary for a subject",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    path = [
      "/{legacySubjectId}",
    ],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getSubject(
    authentication: Authentication,
    @Parameter(description = "The legacy subject ID", required = true)
    @Pattern(regexp = "^[0-9]+$", message = "Input contains illegal characters - legacy subject ID must be a number")
    @PathVariable legacySubjectId: String,
  ): ResponseEntity<SubjectInformation> {
    val validatedRole = athenaRoleService.getRoleFromAuthentication(authentication)

    val result = subjectService.getSubjectInformation(legacySubjectId, validatedRole)

    auditService.createEvent(
      authentication.name,
      "GET_ORDER_SUMMARY",
      mapOf(
        "legacySubjectId" to legacySubjectId,
        "restrictedOrdersIncluded" to false,
      ),
    )

    return ResponseEntity.ok(result)
  }
}