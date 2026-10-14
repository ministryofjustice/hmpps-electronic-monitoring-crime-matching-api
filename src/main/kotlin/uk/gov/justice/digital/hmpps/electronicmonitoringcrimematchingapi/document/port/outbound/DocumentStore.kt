package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.port.outbound

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentArtefact
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument

interface DocumentStore {
  fun save(document: GeneratedDocument): DocumentArtefact
}
