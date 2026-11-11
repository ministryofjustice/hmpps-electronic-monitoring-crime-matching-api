package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport

interface CrimeMatchingResultDocumentRequestStore {
  fun save(link: CrimeMatchingResultDocumentRequest)
}
