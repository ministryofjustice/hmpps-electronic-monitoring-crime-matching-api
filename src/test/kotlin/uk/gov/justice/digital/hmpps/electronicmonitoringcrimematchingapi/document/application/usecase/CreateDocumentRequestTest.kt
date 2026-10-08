package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.CreateDocumentRequestCommand
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.mapper.DocumentRequestMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.parser.DocumentRequestParameterParser
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.ContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.InMemoryDocumentRequestStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CreateDocumentRequestTest {
  private val store = InMemoryDocumentRequestStore()

  private val clock = Clock.fixed(
    Instant.parse("2026-10-08T12:00:00Z"),
    ZoneOffset.UTC,
  )

  private val useCase = CreateDocumentRequest(
    store = store,
    mapper = DocumentRequestMapper(),
    parser = DocumentRequestParameterParser(),
    clock = clock,
  )

  @Test
  fun `it should create and persist a pending contextual report request`() {
    // Given a valid command
    val command = givenCreateDocumentRequestCommand()

    // When a document request is created
    val result = useCase.create(command, "user123")

    // Then the document request should be persisted
    val saved = store.findById(java.util.UUID.fromString(result.id))

    assertThat(saved).isNotNull
    assertThat(saved!!.id.toString()).isEqualTo(result.id)
    assertThat(saved.type).isEqualTo(DocumentType.CONTEXTUAL_REPORT)
    assertThat(saved.status).isEqualTo(DocumentRequestStatus.PENDING)
    assertThat(saved.createdBy).isEqualTo("user123")
    assertThat(saved.createdAt).isEqualTo(clock.instant())

    assertThat(saved.parameters).isEqualTo(
      ContextualReportRequestParameters(
        deviceId = 123456,
        from = Instant.parse("2026-10-01T00:00:00Z"),
        to = Instant.parse("2026-10-02T00:00:00Z"),
      ),
    )
  }

  @Test
  fun `it should not persist a request with invalid parameters`() {
    // Given a command with invalid parameters
    val invalidCommand = givenCreateDocumentRequestCommand(
      parameters = mapOf(
        "deviceId" to -1, // Invalid deviceId
        "from" to "2026-10-01T00:00:00Z",
        "to" to "2026-10-02T00:00:00Z",
      ),
    )

    // When a document request is created, then it should throw an exception and not persist the request
    assertThatThrownBy {
      useCase.create(invalidCommand, "user123")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("'deviceId' must be a positive integer")

    assertThat(store.findAll()).isEmpty()
  }

  @Test
  fun `it should create independent document requests`() {
    // Given a valid command
    val command = givenCreateDocumentRequestCommand()

    // When two document requests are created with the same command
    val first = useCase.create(command, "user123")
    val second = useCase.create(command, "user123")

    // Then the two requests should have been created
    assertThat(first.id).isNotEqualTo(second.id)
    assertThat(store.findAll()).hasSize(2)
  }

  private fun givenCreateDocumentRequestCommand(
    parameters: Map<String, Any> = mapOf(
      "deviceId" to 123456,
      "from" to "2026-10-01T00:00:00Z",
      "to" to "2026-10-02T00:00:00Z",
    ),
  ): CreateDocumentRequestCommand = CreateDocumentRequestCommand(
    type = DocumentType.CONTEXTUAL_REPORT,
    parameters = parameters,
  )
}
