package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.generator.DocumentGeneratorRegistry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentStore

class ProcessDocumentRequest(
  private val generators: DocumentGeneratorRegistry,
  private val documentStore: DocumentStore,
  private val requestStore: DocumentRequestStore,
) {
  fun process(request: DocumentRequest) {
    request.startProcessing()
    requestStore.save(request)

    val generator = generators.get(request.type)
    val document = generator.generate(request)

    val artefact = documentStore.save(document)

    request.complete(artefact)
    requestStore.save(request)
  }
}
