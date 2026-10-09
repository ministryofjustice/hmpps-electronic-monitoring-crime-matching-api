package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.persistence

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import java.util.UUID

@Component
class DocumentRequestPersistenceAdapter(
  private val repository: DocumentRequestRepository,
  private val mapper: DocumentRequestPersistenceMapper,
) : DocumentRequestStore {

  override fun findById(id: UUID): DocumentRequest? = repository.findById(id)
    .orElse(null)
    ?.let(mapper::toDomain)

  override fun save(documentRequest: DocumentRequest): DocumentRequest {
    val entity = mapper.toEntity(documentRequest)
    val saved = repository.save(entity)
    return mapper.toDomain(saved)
  }
}
