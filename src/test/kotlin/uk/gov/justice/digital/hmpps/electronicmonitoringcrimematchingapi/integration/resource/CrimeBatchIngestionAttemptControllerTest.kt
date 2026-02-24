package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures.TestFixturesConfig
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles("integration")
@Import(TestFixturesConfig::class)
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
      createBatch()

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
        "get-ingestion-attempt-summary-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return the second page of ingestion attempt summaries`() {
      createBatch()
      crimeMatchingFixtures.givenBatch(
        ingestionAttemptId = UUID.fromString("aefa6993-2bed-4e69-a96e-afb572046a6f"),
        ingestionCreatedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
        batchId = "Batch2",
      ) {
        withCrime("crime2") {
          withMatchingRun {
            withMatchedDeviceWearer(deviceId = 2)
          }
        }
      }

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
        "get-ingestion-attempt-summary-second-page-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should filter ingestion attempt summaries by batchId`() {
      createBatch()

      crimeMatchingFixtures.givenBatch(batchId = "Batch2") {
        withCrime("crime2") {
          withMatchingRun {
            withMatchedDeviceWearer(deviceId = 2)
          }
        }
      }

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
        "get-ingestion-attempt-summary-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should filter ingestion attempt summaries by policeForce`() {
      createBatch()
      crimeMatchingFixtures.givenBatch(batchId = "Batch2", policeForce = PoliceForce.BEDFORDSHIRE) {
        withCrime("crime2") {
          withMatchingRun {
            withMatchedDeviceWearer(deviceId = 2)
          }
        }
      }

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
        "get-ingestion-attempt-summary-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should filter ingestion attempt summaries by fromDate`() {
      createBatch()

      crimeMatchingFixtures.givenBatch(batchId = "Batch2", ingestionCreatedAt = LocalDateTime.of(2024, 1, 1, 0, 0)) {
        withCrime("crime2") {
          withMatchingRun {
            withMatchedDeviceWearer(deviceId = 2)
          }
        }
      }

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
        "get-ingestion-attempt-summary-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should filter ingestion attempt summaries by toDate`() {
      createBatch()

      crimeMatchingFixtures.givenBatch(batchId = "Batch2", ingestionCreatedAt = LocalDateTime.of(2026, 1, 1, 0, 0)) {
        withCrime("crime2") {
          withMatchingRun {
            withMatchedDeviceWearer(deviceId = 2)
          }
        }
      }

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
        "get-ingestion-attempt-summary-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return a failed ingestion attempt summary`() {
      crimeMatchingFixtures.givenBatch(ingestionAttemptId = UUID.fromString("aefa6993-2bed-4e69-a96e-afb562046a6f"), batchId = "Batch1") {}

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
        "get-failed-ingestion-attempt-summary-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return a partially failed ingestion attempt summary`() {
      crimeMatchingFixtures.givenBatch(ingestionAttemptId = UUID.fromString("aefa6993-2bed-4e69-a96e-afb562046a6f"), batchId = "Batch1", rowCount = 2) {
        withCrime("crime1") {
          withMatchingRun {
            withMatchedDeviceWearer(deviceId = 1)
          }
        }
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
        "get-partial-ingestion-attempt-summary-response".loadJson(),
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

    private fun createBatch() {
      val ingestionAttemptId = UUID.fromString("aefa6993-2bed-4e69-a96e-afb562046a6f")
      crimeMatchingFixtures.givenBatch(ingestionAttemptId = ingestionAttemptId, batchId = "Batch1") {
        withCrime("crime1") {
          withMatchingRun {
            withMatchedDeviceWearer(deviceId = 1)
          }
        }
      }
    }
  }
}
