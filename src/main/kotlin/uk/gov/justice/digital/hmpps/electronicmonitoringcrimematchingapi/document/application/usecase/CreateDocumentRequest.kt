package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.CreateDocumentRequestCommand
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.DocumentRequestSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.mapper.DocumentRequestMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.parser.DocumentRequestParameterParser
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import java.time.Clock

@Service
class CreateDocumentRequest(
  private val store: DocumentRequestStore,
  private val mapper: DocumentRequestMapper,
  private val parser: DocumentRequestParameterParser,
  private val clock: Clock,
  private val processNextDocumentRequest: ProcessNextDocumentRequest,
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

    val summary = mapper.toSummary(store.save(documentRequest))

    try {
      processNextDocumentRequest.process()
    } catch (exception: Exception) {
      log.error("Unable to dispatch document processing for request {}; request remains pending", summary.id, exception)
    }
    // TODO(ACR-1101): Add a scheduled job to recover pending requests.
    return summary
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
