package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.inbound.web

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(
  name = "Document requests",
  description = "Create, retrieve and download generated documents",
)
@PreAuthorize("hasAnyAuthority('ROLE_EM_CRIME_MATCHING__DOCUMENT_REQUESTS__RW')")
@RequestMapping("/document-requests")
class DocumentRequestController(
  private val createDocumentRequest: CreateDocumentRequest,
  private val getDocumentRequest: GetDocumentRequest,
  private val listDocumentRequests: ListDocumentRequests,
  private val getDocumentRequestArtefact: GetDocumentRequestArtefact,
) {
  @PostMapping
  @Operation(
    summary = "Create a document request",
    description = "Submit a request to generate a document.",
  )
  @ApiResponse(
    responseCode = "202",
    description = "Document request accepted",
  )
  @ApiResponse(
    responseCode = "400",
    description = "Invalid document request",
    content = [Content()],
  )
  fun createDocumentRequest(
    @RequestBody
    request: CreateDocumentRequestCommand,
    @Parameter(hidden = true)
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
  @Operation(
    summary = "Get a document request",
    description = "Retrieve a document request by its ID.",
  )
  @ApiResponse(
    responseCode = "200",
    description = "Document request retrieved",
  )
  @ApiResponse(
    responseCode = "404",
    description = "Document request not found",
    content = [Content()],
  )
  fun getDocumentRequest(
    @Parameter(description = "Document request ID")
    @PathVariable documentRequestId: UUID,
  ): ResponseEntity<DocumentRequestSummary> {
    val result = getDocumentRequest.get(documentRequestId)

    return ResponseEntity.ok(result)
  }

  @GetMapping
  @Operation(
    summary = "List document requests",
    description = "Retrieve a paginated list of document requests.",
  )
  @ApiResponse(
    responseCode = "200",
    description = "Document requests retrieved",
  )
  fun listDocumentRequests(
    @Parameter(description = "Zero-based page number", example = "0")
    @RequestParam(defaultValue = "0") page: Int,
    @Parameter(description = "Number of results per page", example = "20")
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
  @Operation(
    summary = "Download a generated document",
    description = "Retrieve the generated document artefact for a document request.",
  )
  @ApiResponse(
    responseCode = "200",
    description = "Generated document retrieved",
    content = [
      Content(
        mediaType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        schema = Schema(type = "string", format = "binary"),
      ),
    ],
  )
  @ApiResponse(
    responseCode = "404",
    description = "Document request or artefact not found",
    content = [Content()],
  )
  fun getDocumentRequestArtefact(
    @Parameter(description = "Document request ID")
    @PathVariable documentRequestId: UUID,
  ): ResponseEntity<ByteArray> {
    val artefact = getDocumentRequestArtefact.get(documentRequestId)

    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(artefact.contentType))
      .body(artefact.content)
  }
}
