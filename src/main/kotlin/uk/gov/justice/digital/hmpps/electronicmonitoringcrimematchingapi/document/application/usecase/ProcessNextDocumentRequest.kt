package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.processnextdocumentrequest.ProcessDocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore

@Component
class ProcessNextDocumentRequest(
  private val documentRequestStore: DocumentRequestStore,
  private val processDocumentRequest: ProcessDocumentRequest,
  @Value("\${document-request.max-attempts}") private val maxAttempts: Int,
) {
  fun process() {
    val documentRequest = documentRequestStore.claimNextEligibleDocumentRequest(
      maxAgeOfClaimBeforeExpiryInSeconds = 10L * 60,
      maxConcurrencyVersion = 2L * maxAttempts, // If a worker has claimed the request more than twice the max attempts without being able to record the result, it is likely that the request is unprocessable and requires manual intervention.
    ) ?: return
    try {
      processDocumentRequest.process(documentRequest)
    } catch (e: Exception) {
      log.warn("Failed to process document request ${documentRequest.id}", e)
      if (documentRequest.concurrencyVersion > 2 * maxAttempts) {
        log.error("Unable to process document request ${documentRequest.id} and unable to record a failure after ${documentRequest.concurrencyVersion} times a worker has claimed the request. If the error is persistent, this requires manual intervention.")
      }
      documentRequest.fail(e, maxAttempts)
      documentRequestStore.save(documentRequest)
      throw e // Rethrow so the exception appears in application logs
    }
  }

  private val log = LoggerFactory.getLogger(this::class.java)
}
