package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentArtefact
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import java.util.*

class InMemoryDocumentStore : DocumentStore {
  private val documents = mutableMapOf<String, GeneratedDocument>()

  override fun save(document: GeneratedDocument): DocumentArtefact {
    val reference = UUID.randomUUID().toString()

    documents[reference] = document

    return DocumentArtefact(
      reference = reference,
      filename = document.filename,
      contentType = document.contentType,
    )
  }

  fun findAll(): Collection<GeneratedDocument> = documents.values

  fun findByReference(reference: String): GeneratedDocument? = documents[reference]
}
