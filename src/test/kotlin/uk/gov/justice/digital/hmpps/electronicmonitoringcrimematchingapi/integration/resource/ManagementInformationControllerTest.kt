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
class ManagementInformationControllerTest : IntegrationTestBase() {
  @Nested
  @DisplayName("GET /management-information/zero-precision-crime-matches")
  inner class GetZeroPrecisionCrimeMatches {
    val path = "/management-information/zero-precision-crime-matches"

    @Test
    fun `it should return 401 if the request is not authenticated`() {
      webTestClient.get()
        .uri(path)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return a 403 if the client does not have a valid role`() {
      webTestClient.get()
        .uri(path)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should return an empty list when no matches exist`() {
      val body = webTestClient.get()
        .uri(path)
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        """
        {
          "data": [],
          "pageCount": 0,
          "pageNumber": 0,
          "pageSize": 10
        }
        """.trimIndent(),
        body.toString(StandardCharsets.UTF_8),
        JSONCompareMode.STRICT,
      )
    }

    @Test
    fun `it should return an empty list when no zero-precision matches exist`() {
      // Given a batch with matching results, but no zero-precision positions
      crimeMatchingFixtures.givenBatch(batchId = "Batch1") {
        withCrime("crime1") {
          withMatchingRun {
            withMatchedDeviceWearer(deviceId = 1) {
              withPosition(precision = 1)
            }
          }
        }
      }

      // When we request zero-precision matches
      val body = webTestClient.get()
        .uri(path)
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      // Then the response should be an empty list
      JSONAssert.assertEquals(
        """
        {
          "data": [],
          "pageCount": 0,
          "pageNumber": 0,
          "pageSize": 10
        }
        """.trimIndent(),
        body.toString(StandardCharsets.UTF_8),
        JSONCompareMode.STRICT,
      )
    }

    @Test
    fun `it should return one row per matched device wearer containing zero-precision positions`() {
      // Given a batch with matching results, and some zero-precision positions
      crimeMatchingFixtures.givenBatch(batchId = "Batch1") {
        withCrime("crime1") {
          withMatchingRun {
            withMatchedDeviceWearer(
              id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
              deviceId = 1,
              deviceSerialNumber = "000000001",
            ) {
              withPosition(precision = 0)
              withPosition(precision = 0)
              withPosition(precision = 10)
            }
            withMatchedDeviceWearer(
              id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
              deviceId = 2,
              deviceSerialNumber = "000000002",
            ) {
              withPosition(precision = 0)
            }
          }
        }
        withCrime("crime2") {
          withMatchingRun {
            withMatchedDeviceWearer(
              id = UUID.fromString("00000000-0000-0000-0000-000000000003"),
              deviceId = 3,
              deviceSerialNumber = "000000003",
            ) {
              withPosition(precision = 0)
            }
          }
        }
      }

      // When we request zero-precision matches
      val body = webTestClient.get()
        .uri(path)
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      // Then the response should contain a count of zero-precision positions for each matched device wearer
      JSONAssert.assertEquals(
        """
        {
          "data": [
          {
            "policeForceArea": "METROPOLITAN",
            "batchId": "Batch1",
            "crimeReference": "crime1",
            "deviceSerialNumber": "000000002",
            "matchedAt": "2025-01-01T01:00:00Z",
            "zeroPrecisionPositionCount": 1
          },
          {
            "policeForceArea": "METROPOLITAN",
            "batchId": "Batch1",
            "crimeReference": "crime1",
            "deviceSerialNumber": "000000001",
            "matchedAt": "2025-01-01T01:00:00Z",
            "zeroPrecisionPositionCount": 2
          },
          {
            "policeForceArea": "METROPOLITAN",
            "batchId": "Batch1",
            "crimeReference": "crime2",
            "deviceSerialNumber": "000000003",
            "matchedAt": "2025-01-01T01:00:00Z",
            "zeroPrecisionPositionCount": 1
          }
  ],
          "pageCount": 1,
          "pageNumber": 0,
          "pageSize": 10
        }
        """.trimIndent(),
        body.toString(StandardCharsets.UTF_8),
        JSONCompareMode.STRICT,
      )
    }
  }
}
