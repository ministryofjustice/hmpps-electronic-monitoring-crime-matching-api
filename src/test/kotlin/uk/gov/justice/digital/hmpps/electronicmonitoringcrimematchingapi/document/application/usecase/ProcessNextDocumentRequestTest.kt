package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.ContextualReportDocxGenerator
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.DocumentGeneratorRegistry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.DocumentSensitivityLabeller
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.processnextdocumentrequest.ProcessDocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class ProcessNextDocumentRequestTest {
  private val clock = Clock.fixed(Instant.parse("2026-10-08T12:00:00Z"), ZoneOffset.UTC)
  private val documentRequestStore = InMemoryDocumentRequestStore()
  private val documentStore = InMemoryDocumentStore()
  private val generator = ContextualReportDocxGenerator(DocumentSensitivityLabeller(clock))
  private val processDocumentRequest = ProcessDocumentRequest(
    generatorRegistry = DocumentGeneratorRegistry(listOf(generator)),
    documentStore = documentStore,
    documentRequestStore = documentRequestStore,
  )

  private val useCase = ProcessNextDocumentRequest(documentRequestStore, processDocumentRequest)

  @Test
  fun `it should generate and persist a contextual report`() {
    // Given a document request
    val request = givenDocumentRequest()
    documentRequestStore.save(request)

    // When the document request is processed
    useCase.process()

    // Then the generated document should be persisted
    assertThat(request.artefact).isNotNull
    val saved = documentStore.findByReference(request.artefact!!.reference)
    assertThat(saved).isNotNull
    assertThat(request.status).isEqualTo(DocumentRequestStatus.COMPLETED)
    val persisted = requireNotNull(documentRequestStore.findById(request.id))
    assertThat(persisted.status).isEqualTo(DocumentRequestStatus.COMPLETED)
    assertThat(persisted.artefact).isEqualTo(request.artefact)
    assertThat(persisted.artefact!!.filename).isEqualTo(saved!!.filename)
    assertThat(persisted.artefact!!.contentType).isEqualTo(saved.contentType)
  }

  @Test
  fun `it should not generate or persist a contextual report if the document request is not in pending state`() {
    // Given a completed document request
    val request = givenDocumentRequest(status = DocumentRequestStatus.COMPLETED)
    documentRequestStore.save(request)

    // When the document request is processed
    useCase.process()

    // Then the generated document should NOT be persisted
    assertThat(request.artefact).isNull()
    assertThat(documentStore.findAll()).isEmpty()
    assertThat(documentRequestStore.findById(request.id)).isNotNull()
  }

  @Test
  fun `it should process only one pending request per invocation`() {
    // Given three pending document requests
    val requests = listOf(givenDocumentRequest(), givenDocumentRequest(), givenDocumentRequest())
    requests.forEach { documentRequestStore.save(it) }

    // When the next document request is processed
    useCase.process()

    // Then only one request should be completed
    assertThat(requests.count { it.status == DocumentRequestStatus.COMPLETED }).isEqualTo(1)
    assertThat(requests.count { it.status == DocumentRequestStatus.PENDING }).isEqualTo(2)
    assertThat(documentStore.findAll()).hasSize(1)

    // When processing is invoked twice more
    useCase.process()
    useCase.process()

    // Then all three requests should be completed with their artefacts persisted
    assertThat(requests).allMatch { it.status == DocumentRequestStatus.COMPLETED }
    assertThat(documentStore.findAll()).hasSize(3)
  }

  private fun givenDocumentRequest(
    status: DocumentRequestStatus = DocumentRequestStatus.PENDING,
  ): DocumentRequest = DocumentRequest.rehydrate(
    id = UUID.randomUUID(),
    type = DocumentType.CONTEXTUAL_REPORT,
    parameters = givenContextualReportRequestParameters(),
    createdBy = "user123",
    createdAt = clock.instant(),
    status = status,
  )
}
