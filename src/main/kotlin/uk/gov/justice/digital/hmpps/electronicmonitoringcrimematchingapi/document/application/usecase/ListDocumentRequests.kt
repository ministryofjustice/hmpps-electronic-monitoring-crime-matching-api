package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.DocumentRequestSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.PaginationRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.PaginationResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.mapper.DocumentRequestMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore

@Service
class ListDocumentRequests(
  private val store: DocumentRequestStore,
  private val mapper: DocumentRequestMapper,
) {
  fun list(
    pagination: PaginationRequest,
  ): PaginationResult<DocumentRequestSummary> = store.findAll(pagination).let { result ->
    PaginationResult(
      data = result.data.map { mapper.toSummary(it) },
      pageCount = result.pageCount,
      pageSize = result.pageSize,
      pageNumber = result.pageNumber,
    )
  }
}
