package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.springframework.http.InvalidMediaTypeException
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.ArtefactFailedToGenerateException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.DocumentRequestArtefactNotFoundException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.DocumentRequestNotFoundException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import java.util.*

@Service
class GetDocumentRequestArtefact(
  private val documentRequestStore: DocumentRequestStore,
  private val documentStore: DocumentStore,
) {
  fun get(id: UUID): GeneratedDocument {
    val documentRequest = documentRequestStore.findById(id) ?: throw DocumentRequestNotFoundException(
      "Document request with id $id not found",
    )

    val artefact = documentRequest.artefact ?: throw DocumentRequestArtefactNotFoundException(
      "Document artefact for request with id $id not found",
    )

    validateContentType(artefact.contentType)
    return documentStore.get(artefact) ?: throw DocumentRequestArtefactNotFoundException(
      "Document artefact for request with id $id not found",
    )
  }

  private fun validateContentType(contentType: String) {
    try {
      // This is a weak check, so we need to trust the DocumentGenerator implementations to get this right.
      MediaType.parseMediaType(contentType)
    } catch (e: InvalidMediaTypeException) {
      throw ArtefactFailedToGenerateException("Invalid content type: $contentType", e)
    }
  }
}
