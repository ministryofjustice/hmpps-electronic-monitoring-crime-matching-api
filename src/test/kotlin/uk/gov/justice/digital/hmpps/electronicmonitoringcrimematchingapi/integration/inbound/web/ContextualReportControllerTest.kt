package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.inbound.web

import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.adapter.outbound.persistence.CrimeMatchingResultDocumentRequestRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.application.dto.CreateContextualReportResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures.createHubManager
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.AlgorithmApiExtension
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.AlgorithmApiExtension.Companion.algorithmApi
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.HubManagerRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@ExtendWith(AlgorithmApiExtension::class)
class ContextualReportControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var hubManagerRepo: HubManagerRepository

  @Autowired
  lateinit var crimeMatchingResultDocumentRequestRepository: CrimeMatchingResultDocumentRequestRepository

  private val role = "ROLE_EM_CRIME_MATCHING__DOCUMENT_REQUESTS__RW"

  @BeforeEach
  fun setup() {
    hubManagerRepo.deleteAll()
    crimeMatchingResultDocumentRequestRepository.deleteAll()
  }

  @Nested
  @DisplayName("POST /matched-wearers/{deviceWearerId}/contextual-reports")
  inner class CreateContextualReport {
    @Test
    fun `it should return a 401 if the request is not authenticated`() {
      val id = UUID.randomUUID()
      webTestClient.post()
        .uri("/matched-wearers/$id/contextual-reports")
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
        .uri("/matched-wearers/$id/contextual-reports")
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
      // Given a valid request and existing matched device wearer
      val deviceWearerId = UUID.randomUUID()
      val start = "2025-01-01T00:00:00Z"
      val end = "2025-01-01T11:00:00Z"
      crimeMatchingFixtures.givenBatch(batchId = "Batch1") {
        withCrime("crime1") {
          withMatchingRun {
            withMatchedDeviceWearer(id = deviceWearerId, deviceId = 1)
          }
        }
        withCrime("crime2") {
          withMatchingRun {
            withMatchedDeviceWearer(id = UUID.randomUUID(), deviceId = 2)
          }
        }
      }
      hubManagerRepo.createHubManager(id = "48b83e4b-ea09-4ba7-8440-a7e5ed534cb4", name = "test manager 1", occupation = "occupation 1", hasSignature = true)
      algorithmApi.stubPositionSegments(
        deviceId = "1",
        startTime = start,
        endTime = end,
      )
      val body = givenCreateContextualReportRequestBody()

      // When the request is sent to the controller, then it should be accepted
      val response = webTestClient
        .post()
        .uri("/matched-wearers/$deviceWearerId/contextual-reports")
        .headers(setAuthorisation(roles = listOf(role)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus()
        .isAccepted
        .expectBody<CreateContextualReportResponse>()
        .returnResult()
        .responseBody!!

      assertThat(response.deviceWearerId).isEqualTo(deviceWearerId.toString())
      assertThat(response.documentRequestId).isNotBlank()

      // And the contextual report request should be persisted
      val request = crimeMatchingResultDocumentRequestRepository.findById(UUID.fromString(response.documentRequestId)).orElseThrow()
      assertThat(request.crimeMatchingResultDeviceWearerId).isEqualTo(deviceWearerId)

      // And the algorithm API should be called with the correct parameters
      algorithmApi.verify(
        1,
        getRequestedFor(urlPathEqualTo("/devices/1/contextual-report-position-segments"))
          .withQueryParam("start_time", equalTo(start))
          .withQueryParam("end_time", equalTo(end)),
      )
    }

    @Test
    fun `it should return a 404 if the device wearer does not exist`() {
      val id = UUID.randomUUID()
      val body = givenCreateContextualReportRequestBody()

      val response = webTestClient.post()
        .uri("/matched-wearers/$id/contextual-reports")
        .headers(setAuthorisation(roles = listOf(role)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult()
        .responseBody!!

      assertThat(response).isEqualTo(
        ErrorResponse(
          status = 404,
          userMessage = "Not Found",
          developerMessage = "No matched wearer with id $id",
        ),
      )
    }

    @Test
    fun `it returns a 400 when the report period exceeds 12 hours`() {
      val deviceWearerId = UUID.randomUUID()
      givenMatchedWearer(deviceWearerId)
      val body = givenCreateContextualReportRequestBody(endTime = "2025-01-02T00:00:01Z")

      val response = webTestClient.post()
        .uri("/matched-wearers/$deviceWearerId/contextual-reports")
        .headers(setAuthorisation(roles = listOf(role)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus().isBadRequest
        .expectBody<ErrorResponse>()
        .returnResult().responseBody!!

      assertThat(response.userMessage)
        .contains("The report time window must not exceed 12 hours")
    }

    @Test
    fun `it returns a 400 when the report period does not overlap with the crime window`() {
      val deviceWearerId = UUID.randomUUID()
      givenMatchedWearer(deviceWearerId)
      val body = givenCreateContextualReportRequestBody(startTime = "2026-01-01T00:00:00Z", endTime = "2026-01-01T11:00:00Z")

      val response = webTestClient.post()
        .uri("/matched-wearers/$deviceWearerId/contextual-reports")
        .headers(setAuthorisation(roles = listOf(role)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus().isBadRequest
        .expectBody<ErrorResponse>()
        .returnResult().responseBody!!

      assertThat(response.developerMessage)
        .contains("The report time window must overlap with the crime time window")
    }

    @Test
    fun `it returns 404 when the hub manager does not exist`() {
      val deviceWearerId = UUID.randomUUID()
      givenMatchedWearer(deviceWearerId)
      val body = givenCreateContextualReportRequestBody()

      val response = webTestClient.post()
        .uri("/matched-wearers/$deviceWearerId/contextual-reports")
        .headers(setAuthorisation(roles = listOf(role)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .exchange()
        .expectStatus().isNotFound
        .expectBody<ErrorResponse>()
        .returnResult().responseBody!!

      assertThat(response.developerMessage)
        .isEqualTo("No hub manager found with id: 48b83e4b-ea09-4ba7-8440-a7e5ed534cb4")
    }

    @Test
    fun `it should not create a contextual report request if the algorithm service is unavailable`() {
      // Given a valid request and the algorithm service is unavailable
      val deviceWearerId = UUID.randomUUID()
      givenMatchedWearer(deviceWearerId)
      hubManagerRepo.createHubManager(id = "48b83e4b-ea09-4ba7-8440-a7e5ed534cb4", name = "test manager 1", occupation = "occupation 1", hasSignature = true)
      algorithmApi.stubPositionSegmentsServiceUnavailable()

      // When the request is sent to the controller then it should be rejected
      webTestClient.post()
        .uri("/matched-wearers/$deviceWearerId/contextual-reports")
        .headers(setAuthorisation(roles = listOf(role)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(givenCreateContextualReportRequestBody())
        .exchange()
        .expectStatus().is5xxServerError

      // And the algorithm API should be called and retried
      algorithmApi.verify(
        3,
        getRequestedFor(urlPathEqualTo("/devices/1/contextual-report-position-segments")),
      )
    }
  }

  private fun givenCreateContextualReportRequestBody(
    startTime: String = "2025-01-01T00:00:00Z",
    endTime: String = "2025-01-01T11:00:00Z",
  ) = mapOf(
    "startTime" to startTime,
    "endTime" to endTime,
    "policeForceArea" to "Metropolitan",
    "officerName" to "Police Officer",
    "requestedAt" to "2026-10-03T11:12:13Z",
    "additionalInformation" to "More information about the request",
    "hubManagerId" to "48b83e4b-ea09-4ba7-8440-a7e5ed534cb4",
  )

  private fun givenMatchedWearer(id: UUID, deviceId: Long = 1) {
    crimeMatchingFixtures.givenBatch(batchId = "Batch1") {
      withCrime("crime1") {
        withMatchingRun {
          withMatchedDeviceWearer(id = id, deviceId = deviceId)
        }
      }
    }
  }
}
