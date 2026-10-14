package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.PaginationRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.mapper.DocumentRequestMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentRequestStore
import java.time.Instant

class ListDocumentRequestsTest {
  private val store = InMemoryDocumentRequestStore()
  private val useCase = ListDocumentRequests(store, DocumentRequestMapper())

  @Test
  fun `it should return a paginated list of document requests`() {
    // Given three document requests
    val requests = (1..3).map { day ->
      givenDocumentRequest(
        createdAt = Instant.parse("2026-10-0${day}T12:00:00Z"),
      ).also(store::save)
    }

    // When the first page is requested
    val result = useCase.list(PaginationRequest(page = 0, size = 2))

    // Then the most recent requests should be returned
    assertThat(result.data.map { it.id }).containsExactly(
      requests[2].id.toString(),
      requests[1].id.toString(),
    )
    assertThat(result.pageCount).isEqualTo(2)
    assertThat(result.pageNumber).isEqualTo(0)
    assertThat(result.pageSize).isEqualTo(2)
  }

  @Test
  fun `it should return an empty page when no document requests exist`() {
    val result = useCase.list(PaginationRequest(page = 0, size = 20))

    assertThat(result.data).isEmpty()
    assertThat(result.pageCount).isZero()
    assertThat(result.pageNumber).isZero()
    assertThat(result.pageSize).isEqualTo(20)
  }

  private fun givenDocumentRequest(createdAt: Instant): DocumentRequest = DocumentRequest.create(
    parameters = givenContextualReportRequestParameters(),
    createdBy = "user123",
    createdAt = createdAt,
  )
}
