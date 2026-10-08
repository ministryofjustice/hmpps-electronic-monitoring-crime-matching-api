package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import java.util.UUID

class InMemoryDocumentRequestStore : DocumentRequestStore {
  private val requests = mutableMapOf<UUID, DocumentRequest>()

  override fun save(documentRequest: DocumentRequest): DocumentRequest {
    requests[documentRequest.id] = documentRequest
    return documentRequest
  }

  fun findById(id: UUID): DocumentRequest? = requests[id]

  fun findAll(): List<DocumentRequest> = requests.values.toList()
}
