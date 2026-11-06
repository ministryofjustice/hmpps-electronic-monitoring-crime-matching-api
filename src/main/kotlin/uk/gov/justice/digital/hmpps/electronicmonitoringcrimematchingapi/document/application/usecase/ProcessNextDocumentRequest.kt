package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

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
      documentRequest.fail(e)
      documentRequestStore.save(documentRequest)
      throw e // Rethrow so the exception appears in application logs
    }
  }
}
