package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.exception.DocumentRequestNotFoundException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.mapper.DocumentRequestMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentRequestStore
import java.time.Instant
import java.util.UUID

class GetDocumentRequestTest {
  private val store = InMemoryDocumentRequestStore()
  private val useCase = GetDocumentRequest(store, DocumentRequestMapper())

  @Test
  fun `it should retrieve an existing document request`() {
    // Given an existing document request
    val request = DocumentRequest.create(
      parameters = ContextualReportRequestParameters(
        deviceId = 123456,
        from = Instant.parse("2026-10-01T08:00:00Z"),
        to = Instant.parse("2026-10-02T08:00:00Z"),
      ),
      createdBy = "user123",
      createdAt = Instant.parse("2026-10-08T12:00:00Z"),
    )

    store.save(request)

    // When the document request is retrieved
    val result = useCase.get(request.id)

    // Then the correct document request is returned
    assertThat(result.id).isEqualTo(request.id.toString())
    assertThat(result.type).isEqualTo(DocumentType.CONTEXTUAL_REPORT)
    assertThat(result.status).isEqualTo(DocumentRequestStatus.PENDING)
    assertThat(result.createdBy).isEqualTo(request.createdBy)
    assertThat(result.createdAt).isEqualTo(request.createdAt)
  }

  @Test
  fun `it should throw when the document request does not exist`() {
    // Given a non-existent document request ID
    val id = UUID.randomUUID()

    // When the document request is retrieved, then a DocumentRequestNotFoundException is thrown
    assertThatThrownBy {
      useCase.get(id)
    }
      .isInstanceOf(DocumentRequestNotFoundException::class.java)
      .hasMessage("Document request with id $id not found")
  }
}
