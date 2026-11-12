package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.testutils

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport.CrimeMatchingResultDocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport.CrimeMatchingResultDocumentRequestStore

class InMemoryCrimeMatchingResultDocumentRequestStore : CrimeMatchingResultDocumentRequestStore {
  val saved = mutableListOf<CrimeMatchingResultDocumentRequest>()

  override fun save(link: CrimeMatchingResultDocumentRequest) {
    saved += link
  }
}
