package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.generator.DocumentGeneratorRegistry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.generator.DocumentGenerator
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentRequestStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ProcessDocumentRequestTest {
  private val store = InMemoryDocumentRequestStore()

  private val clock = Clock.fixed(
    Instant.parse("2026-10-08T12:00:00Z"),
    ZoneOffset.UTC,
  )

  private val documentRequestStore = mock<DocumentRequestStore> {
    on { claimEligibleDocumentRequests(eq(1), any(), any()) } doAnswer {
      store.claimEligibleDocumentRequests(
        batchSize = it.getArgument(0),
        cutoff = it.getArgument(1),
        now = it.getArgument(2),
      )
    }
    on { save(any()) } doAnswer {
      store.save(it.getArgument(0))
    }
  }

  private val generator = mock<DocumentGenerator> {
    on { type }.thenReturn(DocumentType.CONTEXTUAL_REPORT)
    on { generate(any()) }.thenReturn(mock<GeneratedDocument>())
  }

  private val useCase = ProcessDocumentRequest(
    documentRequestStore = documentRequestStore,
    generators = DocumentGeneratorRegistry(listOf(generator)),
    clock = clock,
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
    val startedAt = Instant.now(clock)

    // When triggerDocumentGeneration is called
    useCase.triggerDocumentGeneration()
    val finishedAt = Instant.now(clock)

    // Then document generation is triggered
    val generatedDocumentRequest = argumentCaptor<DocumentRequest>()
    verify(documentRequestStore).claimEligibleDocumentRequests(eq(1), cutoff.capture(), now.capture())
    verify(generator, times(1)).generate(generatedDocumentRequest.capture())

    assertThat(now.firstValue).isBetween(startedAt, finishedAt)
    assertThat(cutoff.firstValue).isEqualTo(now.firstValue.minusSeconds(60 * 5))
    assertThat(generatedDocumentRequest.firstValue.id).isEqualTo(documentRequest.id)
    assertThat(generatedDocumentRequest.firstValue.claimedAt).isEqualTo(now.firstValue)
    assertThat(generatedDocumentRequest.firstValue.concurrencyVersion).isEqualTo(1L)
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

    verify(documentRequestStore).claimEligibleDocumentRequests(eq(1), cutoff.capture(), now.capture())
    verify(generator, times(1)).generate(generatedDocumentRequests.capture())

    assertThat(generatedDocumentRequests.firstValue.id).isIn(firstDocumentRequest.id, secondDocumentRequest.id)
    assertThat(generatedDocumentRequests.firstValue.claimedAt).isEqualTo(now.firstValue)
    assertThat(generatedDocumentRequests.firstValue.concurrencyVersion).isEqualTo(1L)
    assertThat(cutoff.firstValue).isEqualTo(now.firstValue.minusSeconds(60 * 5))
  }

  @Test
  fun `it should not trigger document generation when no document requests are eligible`() {
    // Given one recent COMPLETED document request in the DB
    val documentRequest = givenDocumentRequest(
      createdAt = Instant.now().minusSeconds(60),
      status = DocumentRequestStatus.COMPLETED,
    )

    store.save(documentRequest)
    val cutoff = argumentCaptor<Instant>()
    val now = argumentCaptor<Instant>()

    // When triggerDocumentGeneration is called
    useCase.triggerDocumentGeneration()

    // Then no document generation is triggered
    verify(documentRequestStore).claimEligibleDocumentRequests(eq(1), cutoff.capture(), now.capture())
    verify(generator, never()).generate(any())

    assertThat(cutoff.firstValue).isEqualTo(now.firstValue.minusSeconds(60 * 5))
  }

  @Test
  fun `it should update the status of the document request to COMPLETED on successful generation`() {
    // Given one eligible PENDING document request in the DB
    val documentRequest = givenDocumentRequest(
      createdAt = Instant.parse("2026-10-01T08:00:00Z"),
    )

    store.save(documentRequest)
    val savedDocumentRequest = argumentCaptor<DocumentRequest>()

    // When triggerDocumentGeneration is called
    useCase.triggerDocumentGeneration()

    // Then the document request is marked as COMPLETED and saved
    verify(documentRequestStore).save(savedDocumentRequest.capture())

    assertThat(savedDocumentRequest.firstValue.id).isEqualTo(documentRequest.id)
    assertThat(savedDocumentRequest.firstValue.status).isEqualTo(DocumentRequestStatus.COMPLETED)
    assertThat(store.findById(documentRequest.id)?.status).isEqualTo(DocumentRequestStatus.COMPLETED)
  }

  @Test
  fun `it should NOT update the status of the document request to COMPLETED on unsuccessful generation`() {
    // Given one eligible PENDING document request in the DB
    val documentRequest = givenDocumentRequest(
      createdAt = Instant.parse("2026-10-01T08:00:00Z"),
    )
    store.save(documentRequest)
    whenever(generator.generate(any())).thenThrow(RuntimeException("generation failed"))

    // When triggerDocumentGeneration is called
    assertThatThrownBy { useCase.triggerDocumentGeneration() }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("generation failed")

    // Then the document request is not marked as COMPLETED or saved
    verify(documentRequestStore, never()).save(any())

    assertThat(documentRequest.status).isEqualTo(DocumentRequestStatus.PENDING)
    assertThat(store.findById(documentRequest.id)?.status).isEqualTo(DocumentRequestStatus.PENDING)
  }

  private fun givenDocumentRequest(createdAt: Instant, status: DocumentRequestStatus = DocumentRequestStatus.PENDING): DocumentRequest = DocumentRequest.rehydrate(
    id = java.util.UUID.randomUUID(),
    type = DocumentType.CONTEXTUAL_REPORT,
    parameters = ContextualReportRequestParameters(
      deviceId = 123456,
      from = Instant.parse("2026-10-01T08:00:00Z"),
      to = Instant.parse("2026-10-02T08:00:00Z"),
    ),
    createdBy = "user123",
    createdAt = createdAt,
    status = status,
    claimedAt = null,
    attempts = 0,
    lastError = null,
    concurrencyVersion = 0L,
  )
}
