package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.generator.DocumentGeneratorRegistry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import java.time.Instant

@Service
class ProcessDocumentRequest(
  private val documentRequestStore: DocumentRequestStore,
  private val generators: DocumentGeneratorRegistry,
) {
  // TODO: run after a new document request and on a schedule (prerequisite retry handling)
  fun triggerDocumentGeneration() {
    // 1. Claim an eligible Contextual Report DocumentRequest to generate
    val now = Instant.now()
    val cutoff = now.minusSeconds(60 * 5)
    val numReportsToGenerate = 1
    val documentRequests: Collection<DocumentRequest> = documentRequestStore.claimEligibleDocumentRequests(
      numReportsToGenerate,
      cutoff,
      now,
    )

    // 2. Request generation of the Contextual Report if any need to be generated
    documentRequests.forEach {
      val type = it.type
      val generatedDocument = generators.get(type).generate(it)
      // TODO(ACR-1102): Save the generated document BEFORE recording the status as COMPLETED
      // This will maintain the At-Least-Once delivery guarantee
      it.status = DocumentRequestStatus.COMPLETED
      documentRequestStore.save(it)
    }
  }
}
