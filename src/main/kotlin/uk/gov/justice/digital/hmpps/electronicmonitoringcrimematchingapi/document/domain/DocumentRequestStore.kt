package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

interface DocumentRequestStore {
  fun save(
    documentRequest: DocumentRequest,
  ): DocumentRequest
}
