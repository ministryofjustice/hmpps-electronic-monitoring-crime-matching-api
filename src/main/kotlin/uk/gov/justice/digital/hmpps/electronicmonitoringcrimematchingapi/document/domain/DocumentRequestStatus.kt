package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

enum class DocumentRequestStatus {
  PENDING,
  COMPLETED,
  FAILED, // Not yet implemented
  DEAD, // Not yet implemented, but is used in GetDocumentRequestArtefact
}
