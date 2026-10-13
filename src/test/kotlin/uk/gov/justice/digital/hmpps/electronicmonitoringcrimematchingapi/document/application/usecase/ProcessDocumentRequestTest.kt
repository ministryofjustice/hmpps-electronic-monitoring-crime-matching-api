package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.generator.ContextualReportGenerator
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.generator.DocumentGeneratorRegistry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.ContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentStore
import java.time.Instant

class ProcessDocumentRequestTest {
  private val requestStore = InMemoryDocumentRequestStore()
  private val documentStore = InMemoryDocumentStore()

  private val useCase = ProcessDocumentRequest(
    generators = DocumentGeneratorRegistry(
      listOf(ContextualReportGenerator()),
    ),
    documentStore = documentStore,
    requestStore = requestStore,
  )

  @Test
  fun `it should generate and store a contextual report`() {
    // Given a pending contextual report request
    val request = DocumentRequest.create(
      parameters = ContextualReportRequestParameters(
        deviceId = 123456,
        from = Instant.parse("2026-10-01T08:00:00Z"),
        to = Instant.parse("2026-10-02T08:00:00Z"),
      ),
      createdBy = "user123",
      createdAt = Instant.parse("2026-10-08T12:00:00Z"),
    )

    requestStore.save(request)

    // When the request is processed
    useCase.process(request)

    // Then the request should be completed
    val saved = requestStore.findById(request.id)!!

    assertThat(saved.status).isEqualTo(DocumentRequestStatus.COMPLETED)
    assertThat(saved.artefact).isNotNull

    // And the document should have been stored
    val document = documentStore.findByReference(saved.artefact!!.reference)

    assertThat(document).isNotNull
    assertThat(document!!.filename).isEqualTo("contextual-report-${request.id}.docx")
    assertThat(document.content).isNotEmpty()
  }
}
