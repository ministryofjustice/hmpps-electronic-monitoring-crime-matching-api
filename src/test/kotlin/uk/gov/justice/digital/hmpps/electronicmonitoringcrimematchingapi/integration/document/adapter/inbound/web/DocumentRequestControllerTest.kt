package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.document.adapter.inbound.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.persistence.DocumentRequestRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.dto.DocumentRequestSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import java.util.UUID

class DocumentRequestControllerTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: DocumentRequestRepository

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
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__DOCUMENT_REQUESTS__RW")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__DOCUMENT_REQUESTS__RW")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__DOCUMENT_REQUESTS__RW")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__DOCUMENT_REQUESTS__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus()
        .isBadRequest

      // And the request should not be persisted
      assertThat(repository.count()).isZero()
    }
  }
}
