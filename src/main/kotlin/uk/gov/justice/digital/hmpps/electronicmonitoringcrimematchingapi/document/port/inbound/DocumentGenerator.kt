package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.port.inbound

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import java.io.ByteArrayOutputStream

interface DocumentGenerator {
  val type: DocumentType
  fun generate(documentRequest: DocumentRequest): ByteArrayOutputStream
}
