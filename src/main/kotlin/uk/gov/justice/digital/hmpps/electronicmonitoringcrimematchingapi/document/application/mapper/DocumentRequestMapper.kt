package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.DocumentRequestSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest

@Component
class DocumentRequestMapper {
  fun toSummary(request: DocumentRequest): DocumentRequestSummary = DocumentRequestSummary(
    id = request.id.toString(),
    type = request.type,
    status = request.status,
    createdBy = request.createdBy,
    createdAt = request.createdAt,
  )
}
