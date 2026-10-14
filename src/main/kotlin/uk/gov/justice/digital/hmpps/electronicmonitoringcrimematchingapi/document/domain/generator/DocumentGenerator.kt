package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.generator

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument

interface DocumentGenerator {
  val type: DocumentType
  fun generate(documentRequest: DocumentRequest): GeneratedDocument
}
