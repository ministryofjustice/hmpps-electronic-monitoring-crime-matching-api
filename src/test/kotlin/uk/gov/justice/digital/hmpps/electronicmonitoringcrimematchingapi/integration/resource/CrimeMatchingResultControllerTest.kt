package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import java.nio.charset.StandardCharsets
import java.util.UUID

@ActiveProfiles("integration")
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
  }
}
