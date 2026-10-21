package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.documentmanagement

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentArtefact
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import java.util.UUID

@Component
class DocumentManagementAdapter(
  private val client: DocumentManagementClient,
) : DocumentStore {
  override fun save(document: GeneratedDocument): DocumentArtefact {
    val documentId = UUID.randomUUID()
    val response = client.upload(documentId, document)

    return DocumentArtefact(
      reference = response.documentUuid.toString(),
      filename = response.documentFilename,
      contentType = response.mimeType,
    )
  }

  override fun get(documentArtefact: DocumentArtefact): GeneratedDocument? = client.get(UUID.fromString(documentArtefact.reference))?.let {
    GeneratedDocument(
      content = it,
      filename = documentArtefact.filename,
      contentType = documentArtefact.contentType,
    )
  }
}
