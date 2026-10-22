package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.DocumentGeneratorRegistry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentStore

@Component
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

    val documentArtefact = documentStore.save(generatedDocument)
    documentRequest.complete(documentArtefact)
  }
}
