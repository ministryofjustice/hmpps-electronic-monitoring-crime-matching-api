package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.inbound.scheduling

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.ProcessNextDocumentRequest

@Component
class DocumentRequestScheduler(
  private val processNextDocumentRequest: ProcessNextDocumentRequest,
) {
  companion object {
    const val EVERY_MINUTE: String = "PT1M"
  }

  // One minute is a compromise between the delay in generating documents
  // and the frequency of database polling.
  // This limits the number of documents that can be generated to a maximum of 60 per replica per hour.
  // For example, if document generation takes four minutes, only 12 (60 / (4 + 1)) will be processed per replica per hour.
  @Scheduled(fixedDelayString = EVERY_MINUTE)
  fun processNextDocumentRequestScheduled() {
    processNextDocumentRequest.process()
  }
}
