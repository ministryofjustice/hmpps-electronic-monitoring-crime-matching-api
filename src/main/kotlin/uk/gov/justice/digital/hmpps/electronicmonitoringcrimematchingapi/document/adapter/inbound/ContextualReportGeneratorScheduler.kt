package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.inbound

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.persistence.DocumentRequestPersistenceAdapter
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.docx.ContextualReportDocxGenerator
import java.time.Instant

@Service
class ContextualReportGeneratorScheduler(
  private val documentRequestPersistenceAdapter: DocumentRequestPersistenceAdapter,
  private val contextualReportDocxGenerator: ContextualReportDocxGenerator,
) {
  companion object {
    const val EVERY_15_MINUTES: String = "PT15M"
  }

  @Scheduled(fixedRateString = EVERY_15_MINUTES)
  fun requestContextualReportGeneration() {
    // 1. Claim an eligible Contextual Report DocumentRequest to generate
    val now = Instant.now()
    val cutoff = now.minusSeconds(60 * 5)
    val numReportsToGenerate = 1
    val documentRequests: Collection<DocumentRequest> = documentRequestPersistenceAdapter.claimEligibleDocumentRequests(
      DocumentType.CONTEXTUAL_REPORT,
      numReportsToGenerate,
      cutoff,
      now
    )

    // 2. Request generation of the Contextual Report if any need to be generated
    documentRequests.forEach {
      contextualReportDocxGenerator.generate(it)
    }
  }
}