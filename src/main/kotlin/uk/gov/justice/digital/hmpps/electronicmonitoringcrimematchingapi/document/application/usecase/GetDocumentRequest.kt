package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.DocumentRequestSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.DocumentRequestNotFoundException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.mapper.DocumentRequestMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import java.util.UUID

@Service
class GetDocumentRequest(
  private val store: DocumentRequestStore,
  private val mapper: DocumentRequestMapper,
) {
  fun get(
    id: UUID,
  ): DocumentRequestSummary {
    val request = store.findById(id) ?: throw DocumentRequestNotFoundException(
      "Document request with id $id not found",
    )

    return mapper.toSummary(request)
  }
}
