package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

interface DocumentGenerator {
  val type: DocumentType

  fun generate(request: DocumentRequest): GeneratedDocument
}
