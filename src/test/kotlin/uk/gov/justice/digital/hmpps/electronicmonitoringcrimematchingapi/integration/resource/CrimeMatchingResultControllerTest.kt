package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures.TestFixturesConfig
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles("integration")
@Import(TestFixturesConfig::class)
class CrimeMatchingResultControllerTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /crime-matching-results")
  inner class GetCrimeMatchingResults {
    @Test
    fun `it should return a 401 if the request is not authenticated`() {
      webTestClient.get()
        .uri("/crime-matching-results")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return a 403 if the client does not have a valid role`() {
      webTestClient.get()
        .uri("/crime-matching-results?batchId=" + UUID.randomUUID())
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should return a 400 if no batch ids in query parameters`() {
      webTestClient.get()
        .uri("/crime-matching-results")
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_MATCHING_RESULTS__RO"),
          ),
        )
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `it should return an empty list if no crime matching results found`() {
      val body = webTestClient.get()
        .uri("/crime-matching-results?batchId=" + UUID.randomUUID())
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_MATCHING_RESULTS__RO"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "empty-list-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return crimes that have matching results`() {
      // Given a crime batch with 2 crimes
      // - crime 1 has 2 matched device wearers
      // - crime 2 has 0 matched device wearers
      val batch = crimeMatchingFixtures.givenBatch("Batch1") {
        withCrime("crime1") {
          withMatchingRun {
            withMatchedDeviceWearer(deviceId = 1)
            withMatchedDeviceWearer(deviceId = 2)
          }
        }
        withCrime("crime2") {
          withMatchingRun() // No matches
        }
      }

      // When the client requests matching results
      val body = webTestClient.get()
        .uri("/crime-matching-results?batchId=" + batch.id)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_MATCHING_RESULTS__RO"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      // Then expect only crime 1 to be in result set
      JSONAssert.assertEquals(
        "get-matching-results-only-matched-crimes-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return the latest matching result for each crime`() {
      // Given a crime batch with 1 crime and 2 matching runs
      // - Matching run 1 matched 1 device wearers
      // - Matching run 2 matched 2 device wearer
      val batch = crimeMatchingFixtures.givenBatch("Batch1") {
        withCrime("crime1") {
          // Older
          withMatchingRun(matchingEnded = LocalDateTime.of(2025, 1, 1, 0, 0)) {
            withMatchedDeviceWearer(deviceId = 1)
          }
          // Newer
          withMatchingRun(matchingEnded = LocalDateTime.of(2025, 1, 2, 0, 0)) {
            withMatchedDeviceWearer(deviceId = 2)
            withMatchedDeviceWearer(deviceId = 3)
          }
        }
      }

      // When the client requests matching results
      val body = webTestClient.get()
        .uri("/crime-matching-results?batchId=" + batch.id)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_MATCHING_RESULTS__RO"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      // Then expect the response to only contain results from the most recent run
      JSONAssert.assertEquals(
        "get-matching-results-most-recent-matches-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return matches for many crime batches`() {
      // Given 2 crime batches with 1 crime and 1 result
      val batch1 = crimeMatchingFixtures.givenBatch("Batch1") {
        withCrime("crime1") {
          withMatchingRun {
            withMatchedDeviceWearer(deviceId = 1)
          }
        }
      }
      val batch2 = crimeMatchingFixtures.givenBatch("Batch2") {
        withCrime("crime2") {
          withMatchingRun {
            withMatchedDeviceWearer(deviceId = 2)
          }
        }
      }

      // When the client requests matching results
      val body = webTestClient.get()
        .uri("/crime-matching-results?batchId=" + batch1.id + "&batchId=" + batch2.id)
        .headers(
          setAuthorisation(
            roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_MATCHING_RESULTS__RO"),
          ),
        )
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      // Then expect a response with matching results for each batch
      JSONAssert.assertEquals(
        "get-matching-results-many-batches-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }
  }
}
