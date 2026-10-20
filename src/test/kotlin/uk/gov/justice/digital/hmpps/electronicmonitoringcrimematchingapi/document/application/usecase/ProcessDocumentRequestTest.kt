package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.ContextualReportDocxGenerator
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.DocumentGeneratorRegistry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.DocumentSensitivityLabeller
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class ProcessDocumentRequestTest {
  private val store = InMemoryDocumentStore()

  private val clock = Clock.fixed(
    Instant.parse("2026-10-08T12:00:00Z"),
    ZoneOffset.UTC,
  )

  private val useCase = ProcessDocumentRequest(
    generatorRegistry = DocumentGeneratorRegistry(
      generators = listOf(
        ContextualReportDocxGenerator(
          DocumentSensitivityLabeller(clock = clock),
        ),
      ),
    ),
    documentStore = store,
  )

  @Test
  fun `it should generate and persist a contextual report`() {
    // Given a document request
    val request = givenDocumentRequest()

    // When the document request is processed
    useCase.process(request)

    // Then the generated document should be persisted
    assertThat(request.artefact).isNotNull
    val saved = store.findByReference(request.artefact!!.reference)
    assertThat(saved).isNotNull
    assertThat(request.status).isEqualTo(DocumentRequestStatus.COMPLETED)
  }

  @Test
  fun `it should not generate or persist a contextual report if the document request is not in pending state`() {
    // Given a completed document request
    val request = givenDocumentRequest(status = DocumentRequestStatus.COMPLETED)

    // When the document request is processed
    assertThrows<IllegalStateException> {
      useCase.process(request)
    }

    // Then the generated document should NOT be persisted
    assertThat(request.artefact).isNull()
    assertThat(store.findAll()).isEmpty()
  }

  private fun givenDocumentRequest(
    status: DocumentRequestStatus = DocumentRequestStatus.PENDING,
  ): DocumentRequest = DocumentRequest.rehydrate(
    id = UUID.randomUUID(),
    type = DocumentType.CONTEXTUAL_REPORT,
    parameters = DocumentRequestParametersFixtures.givenContextualReportRequestParameters(),
    createdBy = "user123",
    createdAt = clock.instant(),
    status = status,
  )
}
