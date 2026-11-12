package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.adapter.outbound.persistence

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport.CrimeMatchingResultDocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport.CrimeMatchingResultDocumentRequestStore

@Component
class CrimeMatchingResultDocumentRequestPersistenceAdapter(
  private val repository: CrimeMatchingResultDocumentRequestRepository,
) : CrimeMatchingResultDocumentRequestStore {
  override fun save(link: CrimeMatchingResultDocumentRequest) {
    repository.save(
      CrimeMatchingResultDocumentRequestEntity(
        documentRequestId = link.documentRequestId,
        crimeMatchingResultDeviceWearerId = link.crimeMatchingResultDeviceWearerId,
      ),
    )
  }
}
