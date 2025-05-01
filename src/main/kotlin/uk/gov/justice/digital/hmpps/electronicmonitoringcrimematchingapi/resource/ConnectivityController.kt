package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource

import io.swagger.v3.oas.annotations.Operation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.AthenaRoleService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.SubjectService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.AuditService

@RestController
class ConnectivityController(
  @Autowired val subjectService: SubjectService,
  val athenaRoleService: AthenaRoleService,
  @Autowired val auditService: AuditService,
) {

  @Operation(
    tags = ["Connectivity"],
    summary = "Test the connectivity with athena",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    path = [
      "/test",
    ],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  @PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING_GENERAL_RO', 'ROLE_TEMPLATE_KOTLIN__UI')")
  fun test(authentication: Authentication): ResponseEntity<Map<String, String>> {
    auditService.createEvent(
      authentication.principal.toString(),
      "CONFIRM_CONNECTION",
      mapOf("confirmConnection" to "true"),
    )

    var message = "API Connection successful"
    try {
      val validatedRole = athenaRoleService.getRoleFromAuthentication(authentication)

      if (!subjectService.checkAvailability(validatedRole)) {
        message = "API Connection successful, but no access to Athena"
      }
    } catch (_: Exception) {
      message = "Error determining Athena access"
    }

    return ResponseEntity(
      mapOf("message" to message),
      HttpStatus.OK,
    )
  }
}
