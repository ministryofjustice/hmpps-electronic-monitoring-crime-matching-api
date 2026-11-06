package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus.COMPLETED
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus.DEAD
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus.FAILED
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus.PENDING
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters
import java.time.Instant
import java.util.UUID

class DocumentRequestTest {
  private val createdAt = Instant.parse("2026-10-08T12:00:00Z")

  private val parameters = givenContextualReportRequestParameters()

  @Test
  fun `it should create a pending document request`() {
    val request = DocumentRequest.create(
      parameters = parameters,
      createdBy = "user123",
      createdAt = createdAt,
    )

    assertThat(request.id).isNotNull()
    assertThat(request.type).isEqualTo(DocumentType.CONTEXTUAL_REPORT)
    assertThat(request.parameters).isEqualTo(parameters)
    assertThat(request.status).isEqualTo(PENDING)
    assertThat(request.createdBy).isEqualTo("user123")
    assertThat(request.createdAt).isEqualTo(createdAt)
    assertThat(request.attempts).isZero()
    assertThat(request.lastError).isNull()
  }

  @Test
  fun `it should restore a persisted document request`() {
    val id = UUID.randomUUID()

    val request = DocumentRequest.rehydrate(
      id = id,
      type = DocumentType.CONTEXTUAL_REPORT,
      parameters = parameters,
      status = PENDING,
      createdBy = "user123",
      createdAt = createdAt,
      claimedAt = null,
      artefact = null,
      attempts = 0,
      lastError = null,
      concurrencyVersion = 0L,
    )

    assertThat(request.id).isEqualTo(id)
    assertThat(request.type).isEqualTo(DocumentType.CONTEXTUAL_REPORT)
    assertThat(request.parameters).isEqualTo(parameters)
    assertThat(request.status).isEqualTo(PENDING)
    assertThat(request.createdBy).isEqualTo("user123")
    assertThat(request.createdAt).isEqualTo(createdAt)
  }

  @Test
  fun `it should record a failed attempt and remain retryable`() {
    val request = givenDocumentRequest()

    request.fail(IllegalStateException("generation failed"), maxAttempts = 3)

    assertThat(request.status).isEqualTo(FAILED)
    assertThat(request.attempts).isEqualTo(1)
    assertThat(request.lastError).isEqualTo("generation failed")
    assertThat(request.artefact).isNull()
  }

  @Test
  fun `it should mark the request dead after the maximum number of attempts`() {
    val maxAttempts = 5
    val request = givenDocumentRequest(attempts = maxAttempts - 1)

    request.fail(IllegalStateException("generation failed"), maxAttempts)

    assertThat(request.status).isEqualTo(DEAD)
    assertThat(request.attempts).isEqualTo(maxAttempts)
    assertThat(request.lastError).isEqualTo("generation failed")
  }

  @Test
  fun `it should complete a failed request and count the successful attempt`() {
    val request = givenDocumentRequest(status = FAILED, attempts = 1)
    val artefact = DocumentArtefact(
      reference = "reference",
      filename = "report.docx",
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    )

    request.complete(artefact)

    assertThat(request.status).isEqualTo(COMPLETED)
    assertThat(request.attempts).isEqualTo(2)
    assertThat(request.artefact).isSameAs(artefact)
  }

  @Test
  fun `it should not fail a completed request`() {
    val request = givenDocumentRequest(status = COMPLETED)

    assertThatThrownBy { request.fail(IllegalStateException("generation failed"), maxAttempts = 3) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("DocumentRequest must be in PENDING or FAILED status")
  }

  private fun givenDocumentRequest(
    status: DocumentRequestStatus = PENDING,
    attempts: Int = 0,
  ): DocumentRequest = DocumentRequest.rehydrate(
    id = UUID.randomUUID(),
    type = DocumentType.CONTEXTUAL_REPORT,
    parameters = parameters,
    status = status,
    createdBy = "user123",
    createdAt = createdAt,
    claimedAt = null,
    artefact = null,
    attempts = attempts,
    lastError = null,
    concurrencyVersion = 0L,
  )
}
