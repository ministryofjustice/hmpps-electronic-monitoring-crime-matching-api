package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.inbound.web

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures.createHubManager
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.AlgorithmApiExtension
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.AlgorithmApiExtension.Companion.algorithmApi
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.HubManagerRepository
import java.util.UUID

@ExtendWith(AlgorithmApiExtension::class)
class ContextualReportControllerTest : IntegrationTestBase() {

  private val role = "ROLE_EM_CRIME_MATCHING__DOCUMENT_REQUESTS__RW"

  @Autowired
  lateinit var hubManagerRepo: HubManagerRepository

  @BeforeEach
  fun setup() {
    hubManagerRepo.deleteAll()
  }

  @Nested
  @DisplayName("POST /crime-matching-results/{crimeMatchingResultId}/matched-wearers/{deviceActivationId}/contextual-reports")
  inner class CreateContextualReport {
    @Test
    fun `it should return a 401 if the request is not authenticated`() {
      val id = UUID.randomUUID()
      webTestClient.post()
        .uri("/crime-matching-results/$id/matched-wearers/1/contextual-reports")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          givenCreateContextualReportRequestBody(),
        )
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return a 403 if the client does not have a valid role`() {
      val id = UUID.randomUUID()
      webTestClient.post()
        .uri("/crime-matching-results/$id/matched-wearers/1/contextual-reports")
        .headers(setAuthorisation(roles = listOf()))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
          givenCreateContextualReportRequestBody(),
        )
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should create a contextual report request`() {
      // Given a valid request
      val body = givenCreateContextualReportRequestBody()
      hubManagerRepo.createHubManager(id = "48b83e4b-ea09-4ba7-8440-a7e5ed534cb4", name = "test manager 1", occupation = "occupation 1", hasSignature = true)
      algorithmApi.stubPositionSegments(
        deviceActivationId = "1",
        startTime = "2026-08-17T12:00:00Z",
        endTime = "2026-08-18T00:00:00Z",
      )

      // When the request is sent to the controller, then it should be accepted
      val id = UUID.randomUUID()
      webTestClient
        .post()
        .uri("/crime-matching-results/$id/matched-wearers/1/contextual-reports")
        .headers(setAuthorisation(roles = listOf(role)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus()
        .isAccepted
        .expectBody<String>()
        .returnResult()
        .responseBody!!
    }
  }

  private fun givenCreateContextualReportRequestBody() = mapOf(
    "startTime" to "2026-08-17T12:00:00Z",
    "endTime" to "2026-08-18T00:00:00Z",
    "policeForceArea" to "Metropolitan",
    "officerName" to "Police Officer",
    "requestedAt" to "2026-10-03T11:12:13Z",
    "additionalInformation" to "More information about the request",
    "hubManagerId" to "48b83e4b-ea09-4ba7-8440-a7e5ed534cb4",
  )
}
