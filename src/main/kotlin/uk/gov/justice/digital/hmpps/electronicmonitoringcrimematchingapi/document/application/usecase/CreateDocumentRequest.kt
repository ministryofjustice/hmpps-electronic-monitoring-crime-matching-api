package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.CreateDocumentRequestCommand
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.DocumentRequestSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.mapper.DocumentRequestMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.parser.DocumentRequestParameterParser
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import java.time.Clock

class CreateDocumentRequest(
  private val store: DocumentRequestStore,
  private val mapper: DocumentRequestMapper,
  private val parser: DocumentRequestParameterParser,
  private val clock: Clock,
) {
  fun create(
    request: CreateDocumentRequestCommand,
    user: String,
  ): DocumentRequestSummary {
    val parameters = parser.parse(
      request.type,
      request.parameters,
    )

    val documentRequest = DocumentRequest.create(
      parameters = parameters,
      createdAt = clock.instant(),
      createdBy = user,
    )

    return mapper.toSummary(store.save(documentRequest))
  }
}
