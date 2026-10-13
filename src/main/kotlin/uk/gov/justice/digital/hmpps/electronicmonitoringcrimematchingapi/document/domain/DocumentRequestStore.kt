package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

import java.time.Instant
import java.util.UUID

interface DocumentRequestStore {
  fun save(
    documentRequest: DocumentRequest,
  ): DocumentRequest

  fun findById(id: UUID): DocumentRequest?

  fun claimEligibleDocumentRequests(
    documentType: DocumentType,
    batchSize: Int,
    cutoff: Instant,
    now: Instant,
  ): Collection<DocumentRequest>
}
