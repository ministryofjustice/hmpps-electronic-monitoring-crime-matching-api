package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType

data class CreateDocumentRequestCommand(
  val parameters: Map<String, Any>,
  val type: DocumentType,
)
