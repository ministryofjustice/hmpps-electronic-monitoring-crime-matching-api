package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.inbound.web

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.CreateDocumentRequestCommand
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.DocumentRequestSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.PaginationRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.PaginationResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.CreateDocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.GetDocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.GetDocumentRequestArtefact
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.ListDocumentRequests
import java.util.UUID

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING__DOCUMENT_REQUESTS__RW')")
@RequestMapping("/document-requests")
class DocumentRequestController(
  private val createDocumentRequest: CreateDocumentRequest,
  private val getDocumentRequest: GetDocumentRequest,
  private val listDocumentRequests: ListDocumentRequests,
  private val getDocumentRequestArtefact: GetDocumentRequestArtefact,
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
    @PathVariable documentRequestId: UUID,
  ): ResponseEntity<DocumentRequestSummary> {
    val result = getDocumentRequest.get(documentRequestId)

    return ResponseEntity.ok(result)
  }

  @GetMapping
  fun listDocumentRequests(
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "20") size: Int,
  ): ResponseEntity<PaginationResult<DocumentRequestSummary>> {
    val result = listDocumentRequests.list(
      PaginationRequest(
        page = page,
        size = size,
      ),
    )

    return ResponseEntity.ok(result)
  }

  @GetMapping("/{documentRequestId}/artefact")
  fun getDocumentRequestArtefact(
    @PathVariable documentRequestId: UUID,
  ): ResponseEntity<ByteArray> {
    val artefact = getDocumentRequestArtefact.get(documentRequestId)

    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(artefact.contentType))
      .body(artefact.content)
  }
}
