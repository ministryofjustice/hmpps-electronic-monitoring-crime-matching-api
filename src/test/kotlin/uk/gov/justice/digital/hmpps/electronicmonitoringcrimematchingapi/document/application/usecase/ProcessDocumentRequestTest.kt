package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.persistence.DocumentRequestPersistenceAdapter
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.generator.DocumentGeneratorRegistry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.port.inbound.DocumentGenerator
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentRequestStore
import java.io.ByteArrayOutputStream
import java.time.Instant

class ProcessDocumentRequestTest {
  private val store = InMemoryDocumentRequestStore()

  private val documentRequestPersistenceAdapter = mock<DocumentRequestPersistenceAdapter> {
    on { claimEligibleDocumentRequests(eq(1), any(), any()) } doAnswer {
      store.claimEligibleDocumentRequests(
        batchSize = it.getArgument(0),
        cutoff = it.getArgument(1),
        now = it.getArgument(2),
      )
    }
  }

  private val generator = mock<DocumentGenerator> {
    on { type }.thenReturn(DocumentType.CONTEXTUAL_REPORT)
    on { generate(any()) }.thenReturn(ByteArrayOutputStream())
  }

  private val useCase = ProcessDocumentRequest(
    documentRequestPersistenceAdapter = documentRequestPersistenceAdapter,
    generators = DocumentGeneratorRegistry(listOf(generator)),
  )

  @Test
  fun `it should trigger document generation`() {
    // Given one PENDING document request in the DB
    val documentRequest = givenDocumentRequest(
      createdAt = Instant.parse("2026-10-01T08:00:00Z"),
    )

    store.save(documentRequest)
    val cutoff = argumentCaptor<Instant>()
    val now = argumentCaptor<Instant>()
    val startedAt = Instant.now()

    // When triggerDocumentGeneration is called
    useCase.triggerDocumentGeneration()
    val finishedAt = Instant.now()

    // Then document generation is triggered
    verify(documentRequestPersistenceAdapter).claimEligibleDocumentRequests(eq(1), cutoff.capture(), now.capture())
    verify(generator, times(1)).generate(documentRequest)

    assertThat(now.firstValue).isBetween(startedAt, finishedAt)
    assertThat(cutoff.firstValue).isEqualTo(now.firstValue.minusSeconds(60 * 5))
  }

  @Test
  fun `it should trigger just one document to generate`() {
    // Given two PENDING document requests in the DB
    val firstDocumentRequest = givenDocumentRequest(
      createdAt = Instant.parse("2026-10-01T08:00:00Z"),
    )
    val secondDocumentRequest = givenDocumentRequest(
      createdAt = Instant.parse("2026-10-01T08:01:00Z"),
    )

    store.save(firstDocumentRequest)
    store.save(secondDocumentRequest)
    val cutoff = argumentCaptor<Instant>()
    val now = argumentCaptor<Instant>()

    // When triggerDocumentGeneration is called
    useCase.triggerDocumentGeneration()

    // Then document generation is triggered for either document, but not both
    val generatedDocumentRequests = argumentCaptor<DocumentRequest>()

    verify(documentRequestPersistenceAdapter).claimEligibleDocumentRequests(eq(1), cutoff.capture(), now.capture())
    verify(generator, times(1)).generate(generatedDocumentRequests.capture())

    assertThat(generatedDocumentRequests.firstValue.id).isIn(firstDocumentRequest.id, secondDocumentRequest.id)
    assertThat(cutoff.firstValue).isEqualTo(now.firstValue.minusSeconds(60 * 5))
  }

  @Test
  fun `it should not trigger document generation when no document requests are eligible`() {
    // Given one recent PENDING document request in the DB
    val documentRequest = givenDocumentRequest(
      createdAt = Instant.now().minusSeconds(60),
    )

    store.save(documentRequest)
    val cutoff = argumentCaptor<Instant>()
    val now = argumentCaptor<Instant>()

    // When triggerDocumentGeneration is called
    useCase.triggerDocumentGeneration()

    // Then no document generation is triggered
    verify(documentRequestPersistenceAdapter).claimEligibleDocumentRequests(eq(1), cutoff.capture(), now.capture())
    verify(generator, never()).generate(any())

    assertThat(cutoff.firstValue).isEqualTo(now.firstValue.minusSeconds(60 * 5))
  }

  private fun givenDocumentRequest(createdAt: Instant): DocumentRequest = DocumentRequest.create(
    parameters = ContextualReportRequestParameters(
      deviceId = 123456,
      from = Instant.parse("2026-10-01T08:00:00Z"),
      to = Instant.parse("2026-10-02T08:00:00Z"),
    ),
    createdBy = "user123",
    createdAt = createdAt,
  )
}
