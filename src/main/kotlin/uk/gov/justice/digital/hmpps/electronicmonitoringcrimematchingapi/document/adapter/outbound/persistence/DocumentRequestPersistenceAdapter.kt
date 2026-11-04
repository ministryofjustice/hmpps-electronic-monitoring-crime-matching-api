package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.persistence

import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.PaginationRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.PaginationResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Component
class DocumentRequestPersistenceAdapter(
  private val repository: DocumentRequestRepository,
  private val mapper: DocumentRequestPersistenceMapper,
  private val clock: Clock,
) : DocumentRequestStore {

  override fun findAll(pagination: PaginationRequest): PaginationResult<DocumentRequest> {
    val results = repository.findAll(
      PageRequest.of(pagination.page, pagination.size, Sort.by(Sort.Direction.DESC, "createdAt")),
    )

    return PaginationResult(
      data = results.content.map(mapper::toDomain),
      pageCount = results.totalPages,
      pageNumber = results.number,
      pageSize = results.size,
    )
  }

  override fun findById(id: UUID): DocumentRequest? = repository.findById(id)
    .orElse(null)
    ?.let(mapper::toDomain)

  @Transactional
  override fun claimNextEligibleDocumentRequest(
    maxAgeOfClaimBeforeExpiryInSeconds: Long,
  ): DocumentRequest? {
    val now = Instant.now(clock)
    val cutoff = now.minusSeconds(maxAgeOfClaimBeforeExpiryInSeconds)
    return repository.claimNextEligibleDocumentRequest(DocumentRequestStatus.PENDING.name, cutoff, now).firstOrNull()
      ?.let(mapper::toDomain)
  }

  override fun save(documentRequest: DocumentRequest): DocumentRequest {
    val entity = mapper.toEntity(documentRequest)
    val saved = repository.save(entity)
    return mapper.toDomain(saved)
  }
}
