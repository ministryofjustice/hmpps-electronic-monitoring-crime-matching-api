package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.mapper

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.DocumentRequestSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest

class DocumentRequestMapper {
  fun toSummary(request: DocumentRequest): DocumentRequestSummary = DocumentRequestSummary(
    id = request.id.toString(),
  )
}
