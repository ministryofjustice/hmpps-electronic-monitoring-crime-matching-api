package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.inbound.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.CreateDocumentRequestCommand
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.DocumentRequestSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.CreateDocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.GetDocumentRequest
import java.util.UUID

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING__DOCUMENT_REQUESTS__RW')")
@RequestMapping("/document-requests")
class DocumentRequestController(
  private val createDocumentRequest: CreateDocumentRequest,
  private val getDocumentRequest: GetDocumentRequest,
) {
  @PostMapping
  fun createDocumentRequest(
    @RequestBody
    request: CreateDocumentRequestCommand,
    authentication: Authentication,
  ): ResponseEntity<DocumentRequestSummary> {
    val result = createDocumentRequest.create(
      request = request,
      user = authentication.name,
    )

    return ResponseEntity
      .status(HttpStatus.ACCEPTED)
      .body(result)
  }

  @GetMapping("/{documentRequestId}")
  fun getDocumentRequest(
    @PathVariable("documentRequestId")
    documentRequestId: UUID,
  ): ResponseEntity<DocumentRequestSummary> {
    val result = getDocumentRequest.get(documentRequestId)

    return ResponseEntity.ok(result)
  }
}
