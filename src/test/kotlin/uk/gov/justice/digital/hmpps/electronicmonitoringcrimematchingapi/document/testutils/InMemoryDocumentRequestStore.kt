package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.PaginationRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.PaginationResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import java.time.Instant
import java.util.*

class InMemoryDocumentRequestStore : DocumentRequestStore {
  private val requests = mutableMapOf<UUID, DocumentRequest>()

  override fun save(documentRequest: DocumentRequest): DocumentRequest {
    requests[documentRequest.id] = documentRequest
    return documentRequest
  }

  override fun findById(id: UUID): DocumentRequest? = requests[id]

  override fun findAll(
    pagination: PaginationRequest,
  ): PaginationResult<DocumentRequest> {
    val sorted = requests.values.sortedWith(
      compareByDescending { it.createdAt },
    )

    val pageCount = (sorted.size + pagination.size - 1) / pagination.size

    return PaginationResult(
      data = sorted.drop(pagination.page * pagination.size)
        .take(pagination.size),
      pageCount = pageCount,
      pageNumber = pagination.page,
      pageSize = pagination.size,
    )
  }

  override fun claimEligibleDocumentRequests(
    batchSize: Int,
    cutoff: Instant,
    now: Instant,
  ): Collection<DocumentRequest> = requests.filter {
    it.value.status == DocumentRequestStatus.PENDING &&
      it.value.createdAt.isBefore(cutoff)
  }
    .values
    .take(batchSize)
}
