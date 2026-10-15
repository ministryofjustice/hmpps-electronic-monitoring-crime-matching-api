package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.artefact

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentArtefact
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument

@Component
class StubDocumentStore : DocumentStore {
  override fun save(document: GeneratedDocument): DocumentArtefact = throw NotImplementedError("StubDocumentStore does not implement save()")

  override fun get(documentArtefact: DocumentArtefact): GeneratedDocument? = throw NotImplementedError("StubDocumentStore does not implement get()")
}
