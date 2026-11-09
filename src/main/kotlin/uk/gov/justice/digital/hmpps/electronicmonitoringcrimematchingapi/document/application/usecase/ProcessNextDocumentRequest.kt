package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.processnextdocumentrequest.ProcessDocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore

@Component
class ProcessNextDocumentRequest(
  private val documentRequestStore: DocumentRequestStore,
  private val processDocumentRequest: ProcessDocumentRequest,
) {
  fun process() {
    val documentRequest = documentRequestStore.claimNextEligibleDocumentRequest(
      maxAgeOfClaimBeforeExpiryInSeconds = 10L * 60,
    ) ?: return
    try {
      processDocumentRequest.process(documentRequest)
    } catch (e: Exception) {
      log.error("Failed to process document request ${documentRequest.id}, concurrency version ${documentRequest.concurrencyVersion} (a high concurrency version could indicate that the configured lease timeout is too short or a permanent failure that hasn't been possible to record to the DB)", e)
      documentRequest.fail(e)
      documentRequestStore.save(documentRequest)
      throw e // Rethrow so the exception appears in application logs
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
