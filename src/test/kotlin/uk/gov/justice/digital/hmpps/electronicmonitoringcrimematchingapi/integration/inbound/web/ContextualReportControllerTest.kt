package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.inbound.web

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
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.persistence.DocumentRequestRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures.createHubManager
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.AlgorithmApiExtension
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.AlgorithmApiExtension.Companion.algorithmApi
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.HubManagerRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingResultDeviceWearerRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingResultRepository
import java.util.UUID

@ExtendWith(AlgorithmApiExtension::class)
class ContextualReportControllerTest : IntegrationTestBase() {

  private val role = "ROLE_EM_CRIME_MATCHING__DOCUMENT_REQUESTS__RW"

  @Autowired
  lateinit var hubManagerRepo: HubManagerRepository

  @Autowired
  lateinit var matchingResultRepo: CrimeMatchingResultRepository

  @Autowired
  lateinit var deviceWearerRepo: CrimeMatchingResultDeviceWearerRepository

  @Autowired
  lateinit var documentRequestRepo: DocumentRequestRepository

  @Autowired
  lateinit var resultDocumentRequestRepo: CrimeMatchingResultDocumentRequestRepository

  @BeforeEach
  fun setup() {
    hubManagerRepo.deleteAll()
    documentRequestRepo.deleteAll()
  }

  @Nested
  @DisplayName("POST /crime-matching-results/{crimeMatchingResultId}/matched-wearers/{deviceId}/contextual-reports")
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
    fun `it should create multiple document requests associated with the result and matched wearer`() {
      // Given a valid request
      val body = givenCreateContextualReportRequestBody()
      val resultId = givenMatchingResultWithWearer()
      val wearerId = deviceWearerRepo.findByCrimeMatchingResultIdAndDeviceId(resultId, 1)!!.id
      hubManagerRepo.createHubManager(id = "48b83e4b-ea09-4ba7-8440-a7e5ed534cb4", name = "test manager 1", occupation = "occupation 1", hasSignature = true)
      algorithmApi.stubPositionSegments(
        deviceId = "1",
        startTime = "2026-08-17T12:00:00Z",
        endTime = "2026-08-18T00:00:00Z",
      )

      // When the request is sent to the controller, then it should be accepted
      val requestIds = (1..2).map {
        webTestClient
          .post()
          .uri("/crime-matching-results/$resultId/matched-wearers/1/contextual-reports")
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

      assertThat(requestIds).doesNotHaveDuplicates()
      assertThat(requestIds.map { documentRequestRepo.findById(UUID.fromString(it)).orElseThrow().id.toString() })
        .containsExactlyInAnyOrderElementsOf(requestIds)
      val links = resultDocumentRequestRepo.findAllByCrimeMatchingResultDeviceWearerId(wearerId)
      assertThat(links.map { it.documentRequestId.toString() }).containsExactlyInAnyOrderElementsOf(requestIds)
      assertThat(links).allSatisfy {
        assertThat(it.crimeMatchingResultId).isEqualTo(resultId)
        assertThat(it.crimeMatchingResultDeviceWearerId).isEqualTo(wearerId)
      }
    }

    @Test
    fun `it should return 404 without creating a document if result and device do not match`() {
      val resultId = givenMatchingResultWithWearer()

      webTestClient.post()
        .uri("/crime-matching-results/$resultId/matched-wearers/2/contextual-reports")
        .headers(setAuthorisation(roles = listOf(role)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(givenCreateContextualReportRequestBody())
        .exchange()
        .expectStatus()
        .isNotFound

      webTestClient.post()
        .uri("/crime-matching-results/${UUID.randomUUID()}/matched-wearers/1/contextual-reports")
        .headers(setAuthorisation(roles = listOf(role)))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(givenCreateContextualReportRequestBody())
        .exchange()
        .expectStatus()
        .isNotFound

      assertThat(documentRequestRepo.count()).isZero()
      assertThat(resultDocumentRequestRepo.count()).isZero()
    }
  }

  private fun givenMatchingResultWithWearer(): UUID {
    crimeMatchingFixtures.givenBatch {
      withCrime(crimeRef = "test-crime") {
        withMatchingRun {
          withMatchedDeviceWearer(deviceId = 1)
        }
      }
    }
    return matchingResultRepo.findAll().single().id
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
