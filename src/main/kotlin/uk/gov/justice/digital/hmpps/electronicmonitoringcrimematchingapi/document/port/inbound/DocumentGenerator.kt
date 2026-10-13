package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.port.inbound

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest

interface DocumentGenerator {
  fun generate(documentRequest: DocumentRequest)
}