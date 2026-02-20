package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import java.nio.charset.StandardCharsets

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
  }
}
