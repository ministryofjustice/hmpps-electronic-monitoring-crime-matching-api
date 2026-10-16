package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.springframework.http.InvalidMediaTypeException
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.ArtefactFailedToGenerateException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.DocumentGeneratorRegistry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentStore

class ProcessDocumentRequest(
  private val generatorRegistry: DocumentGeneratorRegistry,
  private val documentStore: DocumentStore,
) {
  fun process(documentRequest: DocumentRequest) {
    check(documentRequest.status == DocumentRequestStatus.PENDING) {
      "DocumentRequest must be in PENDING status"
    }

    val type = documentRequest.type
    val generatedDocument = generatorRegistry.get(type).generate(documentRequest)

    validateContentType(generatedDocument.contentType)
    val documentArtefact = documentStore.save(generatedDocument)
    documentRequest.complete(documentArtefact)
  }

  private fun validateContentType(contentType: String) {
    try {
      MediaType.parseMediaType(contentType)
    } catch (e: InvalidMediaTypeException) {
      throw ArtefactFailedToGenerateException("Invalid content type: $contentType", e)
    }
  }
}
