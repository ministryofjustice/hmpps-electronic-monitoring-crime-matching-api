package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles("integration")
class CrimeBatchIngestionAttemptControllerTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /ingestion-attempts")
  inner class GetCrimeBatchIngestionAttempts {
    @Test
    fun `it should return a 401 if the request is not authenticated`() {
      webTestClient.get()
        .uri("/ingestion-attempts")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return a 403 if the client does not have a valid role`() {
      webTestClient.get()
        .uri("/ingestion-attempts")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should return an empty list if no crime batch ingestion attempts found`() {
      val body = webTestClient.get()
        .uri("/ingestion-attempts")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "empty-page-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return ingestion attempt summaries`() {
      createIngestionAttemptWithBatch()

      val body = webTestClient.get()
        .uri("/ingestion-attempts")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-ingestion-attempts-successful-ingestion-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return the second page of ingestion attempt summaries`() {
      createIngestionAttemptWithBatch()

      crimeMatchingFixtures.givenIngestionAttempt(
        ingestionCreatedAt = LocalDateTime.of(2026, 1, 2, 0, 0),
      )

      val body = webTestClient.get()
        .uri("/ingestion-attempts?page=1&pageSize=1")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-ingestion-attempts-second-page-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should filter ingestion attempt summaries by batchId`() {
      createIngestionAttemptWithBatch()
      crimeMatchingFixtures.givenBatch(batchId = "Batch2")

      val body = webTestClient.get()
        .uri("/ingestion-attempts?batchId=Batch1")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-ingestion-attempts-successful-ingestion-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should filter ingestion attempt summaries by policeForce`() {
      createIngestionAttemptWithBatch()
      crimeMatchingFixtures.givenBatch(policeForce = PoliceForce.BEDFORDSHIRE)

      val body = webTestClient.get()
        .uri("/ingestion-attempts?policeForceArea=METROPOLITAN")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-ingestion-attempts-successful-ingestion-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should filter ingestion attempt summaries by fromDate`() {
      createIngestionAttemptWithBatch()

      crimeMatchingFixtures.givenIngestionAttempt(
        ingestionCreatedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
      )

      val body = webTestClient.get()
        .uri("/ingestion-attempts?fromDate=2025-01-01T00:00:00")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-ingestion-attempts-successful-ingestion-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should filter ingestion attempt summaries by toDate`() {
      createIngestionAttemptWithBatch()

      crimeMatchingFixtures.givenIngestionAttempt(
        ingestionCreatedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
      )

      val body = webTestClient.get()
        .uri("/ingestion-attempts?toDate=2025-01-02T00:00:00")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-ingestion-attempts-successful-ingestion-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return a failed ingestion attempt summary`() {
      val ingestionAttemptId = UUID.fromString("aefa6993-2bed-4e69-a96e-afb562046a6f")
      crimeMatchingFixtures.givenIngestionAttempt(ingestionAttemptId = ingestionAttemptId)

      val body = webTestClient.get()
        .uri("/ingestion-attempts")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-ingestion-attempts-failed-ingestion-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return a partially failed ingestion attempt summary`() {
      val batchId = UUID.fromString("22134a17-c192-4475-88ab-39d90c92f036")
      val ingestionAttemptId = UUID.fromString("aefa6993-2bed-4e69-a96e-afb562046a6f")
      val ingestionAttempt = crimeMatchingFixtures.givenIngestionAttempt(rowCount = 2, ingestionAttemptId = ingestionAttemptId)

      crimeMatchingFixtures.givenBatch(crimeBatchId = batchId, ingestionAttempt = ingestionAttempt, batchId = "Batch1") {
        withCrime("crime1") {}
      }

      val body = webTestClient.get()
        .uri("/ingestion-attempts")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-ingestion-attempts-partial-ingestion-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return a BAD_REQUEST response if from date is not valid`() {
      val body = webTestClient.get()
        .uri("/ingestion-attempts?fromDate=abc")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ErrorResponse>()
        .returnResult()
        .responseBody!!

      assertThat(body).isEqualTo(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "The provided value 'abc' is the incorrect type for the 'fromDate' parameter.",
          developerMessage = "The provided value 'abc' is the incorrect type for the 'fromDate' parameter.",
        ),
      )
    }

    @Test
    fun `it should return a BAD_REQUEST response if to date is not valid`() {
      val body = webTestClient.get()
        .uri("/ingestion-attempts?toDate=abc")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ErrorResponse>()
        .returnResult()
        .responseBody!!

      assertThat(body).isEqualTo(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "The provided value 'abc' is the incorrect type for the 'toDate' parameter.",
          developerMessage = "The provided value 'abc' is the incorrect type for the 'toDate' parameter.",
        ),
      )
    }
  }

  @Nested
  @DisplayName("GET /ingestion-attempts/{ingestionAttemptId}")
  inner class GetCrimeBatchIngestionAttempt {
    @Test
    fun `it should return a 401 if the request is not authenticated`() {
      webTestClient.get()
        .uri("/ingestion-attempts/1")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return a 403 if the client does not have a valid role`() {
      webTestClient.get()
        .uri("/ingestion-attempts/" + UUID.randomUUID())
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should return a 404 if the ingestion attempt does not exist`() {
      val id = UUID.randomUUID()
      webTestClient.get()
        .uri("/ingestion-attempts/$id")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO")))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("$.developerMessage").isEqualTo("No crime batch ingestion attempt found with id: $id")
        .jsonPath("$.userMessage").isEqualTo("Not Found")
    }

    @Test
    fun `it should return an ingestion attempt if it exists`() {
      createIngestionAttemptWithBatch()

      // Validate ingestion attempt is retrieved successfully
      val body = webTestClient.get()
        .uri("/ingestion-attempts/aefa6993-2bed-4e69-a96e-afb562046a6f")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-ingestion-attempt-successful-ingestion-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return a failed ingestion attempt`() {
      crimeMatchingFixtures.givenIngestionAttempt(ingestionAttemptId = UUID.fromString("aefa6993-2bed-4e69-a96e-afb562046a6f"))

      val body = webTestClient.get()
        .uri("/ingestion-attempts/aefa6993-2bed-4e69-a96e-afb562046a6f")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-ingestion-attempt-failed-ingestion-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return a partial ingestion attempt`() {
      val ingestionAttempt = crimeMatchingFixtures.givenIngestionAttempt(
        rowCount = 2,
        ingestionAttemptId = UUID.fromString("aefa6993-2bed-4e69-a96e-afb562046a6f"),
      ) {
        withAttachmentIngestionError()
      }

      val batchId = UUID.fromString("22134a17-c192-4475-88ab-39d90c92f036")
      crimeMatchingFixtures.givenBatch(crimeBatchId = batchId, ingestionAttempt = ingestionAttempt, batchId = "Batch1") {
        withCrime("crime1") {}
      }

      val body = webTestClient.get()
        .uri("/ingestion-attempts/aefa6993-2bed-4e69-a96e-afb562046a6f")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-ingestion-attempt-partial-ingestion-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return a BAD_REQUEST response if ingestion attempt id is not valid`() {
      val body = webTestClient.get()
        .uri("/ingestion-attempts/abc")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO")))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ErrorResponse>()
        .returnResult()
        .responseBody!!

      assertThat(body).isEqualTo(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "The provided value 'abc' is the incorrect type for the 'crimeBatchIngestionAttemptId' parameter.",
          developerMessage = "The provided value 'abc' is the incorrect type for the 'crimeBatchIngestionAttemptId' parameter.",
        ),
      )
    }
  }

  private fun createIngestionAttemptWithBatch(
    ingestionAttemptId: UUID = UUID.fromString("aefa6993-2bed-4e69-a96e-afb562046a6f"),
    batchId: UUID = UUID.fromString("22134a17-c192-4475-88ab-39d90c92f036"),
  ) {
    val ingestionAttempt = crimeMatchingFixtures.givenIngestionAttempt(ingestionAttemptId = ingestionAttemptId)

    crimeMatchingFixtures.givenBatch(crimeBatchId = batchId, ingestionAttempt = ingestionAttempt, batchId = "Batch1") {
      withCrime("crime1") {
        withMatchingRun {
          withMatchedDeviceWearer(deviceId = 1)
        }
      }
    }
  }
}
