package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.document.adapter.inbound.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.persistence.DocumentRequestEntity
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.persistence.DocumentRequestRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.DocumentRequestSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import java.time.Instant
import java.util.UUID

class DocumentRequestControllerTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: DocumentRequestRepository

  private val role = "ROLE_EM_CRIME_MATCHING__DOCUMENT_REQUESTS__RW"

  @BeforeEach
  fun setUp() {
    repository.deleteAll()
  }

  @Nested
  @DisplayName("POST /document-requests")
  inner class CreateDocumentRequest {
    @Test
    fun `it should return a 401 if the request is not authenticated`() {
      webTestClient.post()
        .uri("/document-requests")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          """
            {
              "type": "CONTEXTUAL_REPORT",
              "parameters": {
                "deviceId": 123456,
                "from": "2026-10-01T08:00:00Z",
                "to": "2026-10-02T08:00:00Z"
              }
            }
          """.trimIndent(),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return a 403 if the client does not have a valid role`() {
      webTestClient.post()
        .uri("/document-requests")
        .headers(setAuthorisation(roles = listOf()))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          """
            {
              "type": "CONTEXTUAL_REPORT",
              "parameters": {
                "deviceId": 123456,
                "from": "2026-10-01T08:00:00Z",
                "to": "2026-10-02T08:00:00Z"
              }
            }
          """.trimIndent(),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should create a contextual report document request`() {
      // Given a valid request
      val body = """
      {
        "type": "CONTEXTUAL_REPORT",
        "parameters": {
          "deviceId": 123456,
          "from": "2026-10-01T08:00:00Z",
          "to": "2026-10-02T08:00:00Z"
        }
      }
      """.trimIndent()

      // When the request is sent to the controller, then it should be accepted
      val request = webTestClient
        .post()
        .uri("/document-requests")
        .headers(setAuthorisation(roles = listOf(role)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus()
        .isAccepted
        .expectBody<DocumentRequestSummary>()
        .returnResult()
        .responseBody!!

      // And the request should be persisted
      val saved = repository.findById(UUID.fromString(request.id)).orElseThrow()

      assertThat(saved.type).isEqualTo(DocumentType.CONTEXTUAL_REPORT)
      assertThat(saved.status).isEqualTo(DocumentRequestStatus.PENDING)
      assertThat(saved.parameters).containsEntry("deviceId", 123456)
      assertThat(saved.parameters).containsEntry(
        "from",
        "2026-10-01T08:00:00Z",
      )
      assertThat(saved.parameters).containsEntry(
        "to",
        "2026-10-02T08:00:00Z",
      )
      assertThat(saved.createdBy).isEqualTo("AUTH_ADM")
    }

    @Test
    fun `it should reject an unsupported document type`() {
      // Given an invalid request (unsupported document type)
      val body = """
      {
        "type": "UNKNOWN",
        "parameters": {}
      }
      """.trimIndent()

      // When the request is sent to the controller, then it should be rejected
      webTestClient.post()
        .uri("/document-requests")
        .headers(setAuthorisation(roles = listOf(role)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus()
        .isBadRequest

      // And the request should not be persisted
      assertThat(repository.count()).isZero()
    }

    @Test
    fun `it should reject a contextual report with an invalid date range`() {
      // Given an invalid request (to before from)
      val body = """
      {
        "type": "CONTEXTUAL_REPORT",
        "parameters": {
          "deviceId": 123456,
          "from": "2026-10-02T08:00:00Z",
          "to": "2026-10-01T08:00:00Z"
        }
      }
      """.trimIndent()

      // When the request is sent to the controller, then it should be rejected
      webTestClient
        .post()
        .uri("/document-requests")
        .headers(setAuthorisation(roles = listOf(role)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus()
        .isBadRequest

      // And the request should not be persisted
      assertThat(repository.count()).isZero()
    }

    @Test
    fun `it should reject a request with missing parameters`() {
      // Given an invalid request (missing 'from' and 'to' parameters)
      val body = """
      {
        "type": "CONTEXTUAL_REPORT",
        "parameters": {
          "deviceId": 123456
        }
      }
      """.trimIndent()

      // When the request is sent to the controller, then it should be rejected
      webTestClient.post()
        .uri("/document-requests")
        .headers(setAuthorisation(roles = listOf(role)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus()
        .isBadRequest

      // And the request should not be persisted
      assertThat(repository.count()).isZero()
    }
  }

  @Nested
  @DisplayName("GET /document-requests/{documentRequestId}")
  inner class GetDocumentRequest {
    @Test
    fun `it should return a 401 if the request is not authenticated`() {
      webTestClient.get()
        .uri("/document-requests/${UUID.randomUUID()}")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return a 403 if the client does not have a valid role`() {
      webTestClient.get()
        .uri("/document-requests/${UUID.randomUUID()}")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should retrieve an existing document request`() {
      // Given an existing document request
      val id = UUID.randomUUID()

      repository.save(
        DocumentRequestEntity(
          id = id,
          type = DocumentType.CONTEXTUAL_REPORT,
          status = DocumentRequestStatus.PENDING,
          parameters = mapOf(
            "deviceId" to 123456,
            "from" to "2026-10-01T08:00:00Z",
            "to" to "2026-10-02T08:00:00Z",
          ),
          createdBy = "test",
          createdAt = Instant.parse("2026-10-08T12:00:00Z"),
        ),
      )

      // When the request is retrieved
      val response = webTestClient.get()
        .uri("/document-requests/$id")
        .headers(setAuthorisation(roles = listOf(role)))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<DocumentRequestSummary>()
        .returnResult()
        .responseBody!!

      // Then the document request is returned
      assertThat(response.id).isEqualTo(id.toString())
      assertThat(response.type).isEqualTo(DocumentType.CONTEXTUAL_REPORT)
      assertThat(response.status).isEqualTo(DocumentRequestStatus.PENDING)
      assertThat(response.createdBy).isEqualTo("test")
      assertThat(response.createdAt).isEqualTo(Instant.parse("2026-10-08T12:00:00Z"))
    }

    @Test
    fun `it should return a 404 if the document request does not exist`() {
      webTestClient.get()
        .uri("/document-requests/${UUID.randomUUID()}")
        .headers(setAuthorisation(roles = listOf(role)))
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `it should return a 400 if the document request ID is not a UUID`() {
      webTestClient.get()
        .uri("/document-requests/abc")
        .headers(setAuthorisation(roles = listOf(role)))
        .exchange()
        .expectStatus()
        .isBadRequest
    }
  }
}
