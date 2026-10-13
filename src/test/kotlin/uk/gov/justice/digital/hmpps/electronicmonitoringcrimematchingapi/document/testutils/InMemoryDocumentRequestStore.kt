package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import java.time.Instant
import java.util.UUID

class InMemoryDocumentRequestStore : DocumentRequestStore {
  private val requests = mutableMapOf<UUID, DocumentRequest>()

  override fun save(documentRequest: DocumentRequest): DocumentRequest {
    requests[documentRequest.id] = documentRequest
    return documentRequest
  }

  override fun findById(id: UUID): DocumentRequest? = requests[id]

  override fun claimEligibleDocumentRequests(
    documentType: DocumentType,
    batchSize: Int,
    cutoff: Instant,
    now: Instant,
  ): Collection<DocumentRequest> {
    throw UnsupportedOperationException()
  }

  fun findAll(): List<DocumentRequest> = requests.values.toList()
}
