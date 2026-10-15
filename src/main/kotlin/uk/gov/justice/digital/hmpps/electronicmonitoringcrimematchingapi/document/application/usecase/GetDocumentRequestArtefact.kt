package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.ArtefactFailedToGenerateException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.DocumentRequestArtefactNotFoundException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.DocumentRequestNotFoundException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import java.util.UUID

@Service
class GetDocumentRequestArtefact(
  private val documentRequestStore: DocumentRequestStore,
  private val documentStore: DocumentStore,
) {
  fun get(id: UUID): GeneratedDocument {
    val documentRequest = documentRequestStore.findById(id) ?: throw DocumentRequestNotFoundException(
      "Document request with id $id not found",
    )

    if (documentRequest.status == DocumentRequestStatus.DEAD) {
      throw ArtefactFailedToGenerateException(
        "Document artefact for request with id $id failed to generate",
      )
    }

    val artefact = documentRequest.artefact ?: throw DocumentRequestArtefactNotFoundException(
      "Document artefact for request with id $id not found",
    )

    return documentStore.get(artefact) ?: throw DocumentRequestArtefactNotFoundException(
      "Document artefact for request with id $id not found",
    )
  }
}
