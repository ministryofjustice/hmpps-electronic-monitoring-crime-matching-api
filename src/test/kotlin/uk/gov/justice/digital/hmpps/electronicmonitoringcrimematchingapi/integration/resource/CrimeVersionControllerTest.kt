package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

@ActiveProfiles("integration")
class CrimeVersionControllerTest : IntegrationTestBase() {

  // Make compared fields explicit and identical across versions unless a test changes them
  private val crimeDateFrom = LocalDateTime.of(2025, 3, 15, 0, 0).toInstant(ZoneOffset.UTC)
  private val crimeDateTo = LocalDateTime.of(2025, 3, 15, 1, 0).toInstant(ZoneOffset.UTC)

  // Explicitly set location fields so they remain identical across versions
  // unless a test intentionally changes them.
  private val latitude = 51.5
  private val longitude = -0.12
  private val easting = 530000.0
  private val northing = 180000.0

  @Nested
  @DisplayName("GET /crime-versions")
  inner class GetCrimeVersions {

    @Test
    fun `it should return 401 if the request is not authenticated`() {
      webTestClient.get()
        .uri("/crime-versions?crimeRef=CRI")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return 403 if the client does not have a valid role`() {
      webTestClient.get()
        .uri("/crime-versions?crimeRef=CRI")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should return an empty result when no crime versions exist`() {
      val body = webTestClient.get()
        .uri("/crime-versions?crimeRef=CRI")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody ?: error("Expected response body but got null")

      JSONAssert.assertEquals(
        "empty-page-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should generate version labels and detect duplicates correctly`() {
      val crimeRef = "01/7298583/25"

      val version1 = UUID.fromString("33333333-3333-3333-3333-333333333333")
      val version2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
      val version3 = UUID.fromString("11111111-1111-1111-1111-111111111111")

      crimeMatchingFixtures.givenBatch(
        policeForce = PoliceForce.HAMPSHIRE,
        batchId = "HAM20251028",
        ingestionCreatedAt = LocalDateTime.of(2025, 10, 28, 4, 3, 1),
      ) {
        withCrime(
          crimeRef = crimeRef,
          id = version1,
          crimeType = CrimeType.AB,
          crimeDateTimeFrom = crimeDateFrom,
          crimeDateTimeTo = crimeDateTo,
          latitude = latitude,
          longitude = longitude,
          easting = easting,
          northing = northing,
        ) {}
      }

      crimeMatchingFixtures.givenBatch(
        policeForce = PoliceForce.HAMPSHIRE,
        batchId = "HAM20251029",
        ingestionCreatedAt = LocalDateTime.of(2025, 10, 29, 10, 3, 1),
      ) {
        withCrime(
          crimeRef = crimeRef,
          id = version2,
          crimeType = CrimeType.BOTD,
          crimeDateTimeFrom = crimeDateFrom,
          crimeDateTimeTo = crimeDateTo,
          latitude = latitude,
          longitude = longitude,
          easting = easting,
          northing = northing,
        ) {
          withMatchingRun {
            withMatchedDeviceWearer(deviceId = 1)
          }
        }
      }

      crimeMatchingFixtures.givenBatch(
        policeForce = PoliceForce.HAMPSHIRE,
        batchId = "HAM20251030",
        ingestionCreatedAt = LocalDateTime.of(2025, 10, 30, 4, 3, 1),
      ) {
        withCrime(
          crimeRef = crimeRef,
          id = version3,
          crimeType = CrimeType.BOTD,
          crimeDateTimeFrom = crimeDateFrom,
          crimeDateTimeTo = crimeDateTo,
          latitude = latitude,
          longitude = longitude,
          easting = easting,
          northing = northing,
        ) {}
      }

      val body = webTestClient.get()
        .uri("/crime-versions?crimeRef=$crimeRef")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody ?: error("Expected response body but got null")

      JSONAssert.assertEquals(
        "get-crime-versions-basic-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return a crime version with a BST crime date in UTC format`() {
      val bstDate = LocalDateTime.of(2025, 4, 1, 1, 0)
        .atZone(ZoneId.of("Europe/London"))
        .toInstant()

      val crimeRef = "01/7298583/25"
      crimeMatchingFixtures.givenBatch {
        withCrime(
          crimeRef = crimeRef,
          crimeDateTimeFrom = bstDate,
        ) {}
      }

      webTestClient.get()
        .uri("/crime-versions?crimeRef=$crimeRef")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.data[0].crimeDate").isEqualTo("2025-04-01T00:00:00Z")
    }

    @Test
    fun `it should return a crime version with a GMT crime date in UTC format`() {
      val gmtDate = LocalDateTime.of(2025, 1, 30, 1, 0)
        .atZone(ZoneId.of("Europe/London"))
        .toInstant()

      val crimeRef = "01/7298583/25"
      crimeMatchingFixtures.givenBatch {
        withCrime(
          crimeRef = crimeRef,
          crimeDateTimeFrom = gmtDate,
        ) {}
      }

      webTestClient.get()
        .uri("/crime-versions?crimeRef=$crimeRef")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.data[0].crimeDate").isEqualTo("2025-01-30T01:00:00Z")
    }

    @Test
    fun `it should detect location updates`() {
      val crimeRef = "01/1234567/25"

      val version1 = UUID.fromString("55555555-5555-5555-5555-555555555555")
      val version2 = UUID.fromString("44444444-4444-4444-4444-444444444444")

      crimeMatchingFixtures.givenBatch(
        policeForce = PoliceForce.HAMPSHIRE,
        batchId = "HAM20251028",
        ingestionCreatedAt = LocalDateTime.of(2025, 10, 28, 4, 3, 1),
      ) {
        withCrime(
          crimeRef = crimeRef,
          id = version1,
          crimeType = CrimeType.AB,
          crimeDateTimeFrom = crimeDateFrom,
          crimeDateTimeTo = crimeDateTo,
          latitude = 51.0,
          longitude = -1.0,
          easting = easting,
          northing = northing,
        ) {}
      }

      crimeMatchingFixtures.givenBatch(
        policeForce = PoliceForce.HAMPSHIRE,
        batchId = "HAM20251029",
        ingestionCreatedAt = LocalDateTime.of(2025, 10, 29, 4, 3, 1),
      ) {
        withCrime(
          crimeRef = crimeRef,
          id = version2,
          crimeType = CrimeType.AB,
          crimeDateTimeFrom = crimeDateFrom,
          crimeDateTimeTo = crimeDateTo,
          latitude = 52.0,
          longitude = -1.5,
          easting = easting,
          northing = northing,
        ) {}
      }

      val body = webTestClient.get()
        .uri("/crime-versions?crimeRef=$crimeRef")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody ?: error("Expected response body but got null")

      JSONAssert.assertEquals(
        "get-crime-versions-location-update-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return YES when a crime version has matching results`() {
      val crimeRef = "01/8888888/25"
      val version = UUID.fromString("66666666-6666-6666-6666-666666666666")

      crimeMatchingFixtures.givenBatch(
        policeForce = PoliceForce.HAMPSHIRE,
        batchId = "HAM20251028",
        ingestionCreatedAt = LocalDateTime.of(2025, 10, 28, 4, 3, 1),
      ) {
        withCrime(
          crimeRef = crimeRef,
          id = version,
          crimeType = CrimeType.AB,
          crimeDateTimeFrom = crimeDateFrom,
          crimeDateTimeTo = crimeDateTo,
          latitude = latitude,
          longitude = longitude,
          easting = easting,
          northing = northing,
        ) {
          withMatchingRun {
            withMatchedDeviceWearer(deviceId = 1)
          }
        }
      }

      val body = webTestClient.get()
        .uri("/crime-versions?crimeRef=$crimeRef")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody ?: error("Expected response body but got null")

      JSONAssert.assertEquals(
        "get-crime-versions-matched-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should support pagination`() {
      val crimeRef = "01/9999999/25"

      val version1 = UUID.fromString("77777777-7777-7777-7777-777777777777")
      val version2 = UUID.fromString("88888888-8888-8888-8888-888888888888")

      crimeMatchingFixtures.givenBatch(
        policeForce = PoliceForce.HAMPSHIRE,
        batchId = "HAM20251028",
        ingestionCreatedAt = LocalDateTime.of(2025, 10, 28, 4, 3, 1),
      ) {
        withCrime(
          crimeRef = crimeRef,
          id = version1,
          crimeType = CrimeType.AB,
          crimeDateTimeFrom = crimeDateFrom,
          crimeDateTimeTo = crimeDateTo,
          latitude = latitude,
          longitude = longitude,
          easting = easting,
          northing = northing,
        ) {}
      }

      crimeMatchingFixtures.givenBatch(
        policeForce = PoliceForce.HAMPSHIRE,
        batchId = "HAM20251029",
        ingestionCreatedAt = LocalDateTime.of(2025, 10, 29, 4, 3, 1),
      ) {
        withCrime(
          crimeRef = crimeRef,
          id = version2,
          crimeType = CrimeType.AB,
          crimeDateTimeFrom = crimeDateFrom,
          crimeDateTimeTo = crimeDateTo,
          latitude = latitude,
          longitude = longitude,
          easting = easting,
          northing = northing,
        ) {}
      }

      val body = webTestClient.get()
        .uri("/crime-versions?crimeRef=$crimeRef&page=1&pageSize=1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody ?: error("Expected response body but got null")

      JSONAssert.assertEquals(
        "get-crime-versions-second-page-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    // This verifies:
    //
    // 1. Duplicate ingestions do not increment version numbers
    // 2. Version numbering continues correctly after a duplicate
    // 3. Multiple fields can appear in the updates list
    // 4. Results are returned newest first
    //
    // Expected order:
    //
    // Latest version
    // Version 2
    // Duplicate
    // Version 1
    @Test
    fun `it should continue version numbering after duplicates and detect multiple field changes`() {
      val crimeRef = "01/7777777/25"

      val version1 = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
      val version2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
      val version3 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
      val version4 = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")

      crimeMatchingFixtures.givenBatch(
        policeForce = PoliceForce.HAMPSHIRE,
        batchId = "HAM20251028",
        ingestionCreatedAt = LocalDateTime.of(2025, 10, 28, 4, 3, 1),
      ) {
        withCrime(
          crimeRef = crimeRef,
          id = version1,
          crimeType = CrimeType.AB,
          crimeDateTimeFrom = crimeDateFrom,
          crimeDateTimeTo = crimeDateTo,
          latitude = latitude,
          longitude = longitude,
          easting = easting,
          northing = northing,
        ) {}
      }

      // Duplicate snapshot
      crimeMatchingFixtures.givenBatch(
        policeForce = PoliceForce.HAMPSHIRE,
        batchId = "HAM20251029",
        ingestionCreatedAt = LocalDateTime.of(2025, 10, 29, 4, 3, 1),
      ) {
        withCrime(
          crimeRef = crimeRef,
          id = version2,
          crimeType = CrimeType.AB,
          crimeDateTimeFrom = crimeDateFrom,
          crimeDateTimeTo = crimeDateTo,
          latitude = latitude,
          longitude = longitude,
          easting = easting,
          northing = northing,
        ) {}
      }

      // Version 2
      crimeMatchingFixtures.givenBatch(
        policeForce = PoliceForce.HAMPSHIRE,
        batchId = "HAM20251030",
        ingestionCreatedAt = LocalDateTime.of(2025, 10, 30, 4, 3, 1),
      ) {
        withCrime(
          crimeRef = crimeRef,
          id = version3,
          crimeType = CrimeType.BOTD,
          crimeDateTimeFrom = crimeDateFrom,
          crimeDateTimeTo = crimeDateTo,
          latitude = latitude,
          longitude = longitude,
          easting = easting,
          northing = northing,
        ) {}
      }

      // Latest version with multiple changes
      crimeMatchingFixtures.givenBatch(
        policeForce = PoliceForce.HAMPSHIRE,
        batchId = "HAM20251031",
        ingestionCreatedAt = LocalDateTime.of(2025, 10, 31, 4, 3, 1),
      ) {
        withCrime(
          crimeRef = crimeRef,
          id = version4,
          crimeType = CrimeType.BIAD,
          crimeDateTimeFrom = crimeDateFrom,
          crimeDateTimeTo = crimeDateTo,
          latitude = 52.0,
          longitude = -1.5,
          easting = easting,
          northing = northing,
        ) {}
      }

      val body = webTestClient.get()
        .uri("/crime-versions?crimeRef=$crimeRef")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody ?: error("Expected response body but got null")

      JSONAssert.assertEquals(
        "get-crime-versions-multiple-derived-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }
  }

  @Nested
  @DisplayName("GET /crime-versions/{crimeVersionId}")
  inner class GetCrimeVersion {
    @Test
    fun `it should return a 401 if the request is not authenticated`() {
      webTestClient.get()
        .uri("/crime-versions/1")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return a 403 if the client does not have a valid role`() {
      webTestClient.get()
        .uri("/crime-versions/" + UUID.randomUUID())
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should return a 404 response if crime version doesn't exist`() {
      val id = UUID.randomUUID()
      webTestClient.get()
        .uri("/crime-versions/$id")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING_GENERAL_RO")))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("$.developerMessage").isEqualTo("No crime version found with id: $id")
        .jsonPath("$.userMessage").isEqualTo("Not Found")
    }

    @Test
    fun `it should return a crime version with no matching results`() {
      val versionId = UUID.fromString("11111111-1111-1111-1111-111111111111")
      crimeMatchingFixtures.givenBatch(batchId = "Batch1") {
        withCrime("crime1", id = versionId) {}
      }

      val body = webTestClient.get()
        .uri("/crime-versions/$versionId")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING_GENERAL_RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-crime-version-no-matching-result-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return a crime version with easting and northing location data`() {
      val versionId = UUID.fromString("11111111-1111-1111-1111-111111111111")
      crimeMatchingFixtures.givenBatch(batchId = "Batch1") {
        withCrime(
          crimeRef = "crime1",
          id = versionId,
          latitude = null,
          longitude = null,
          easting = easting,
          northing = northing,
        ) {}
      }

      val body = webTestClient.get()
        .uri("/crime-versions/$versionId")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING_GENERAL_RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-crime-version-easting-northing-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return a crime version with matching results`() {
      val versionId = UUID.fromString("11111111-1111-1111-1111-111111111111")
      crimeMatchingFixtures.givenBatch(batchId = "Batch1") {
        withCrime("crime1", id = versionId) {
          withMatchingRun {
            withMatchedDeviceWearer(deviceId = 1) {
              withPosition(sequenceLabel = "A2", capturedDateTime = LocalDateTime.of(2025, 1, 1, 2, 0))
              withPosition()
            }
            withMatchedDeviceWearer(deviceId = 2) {
              withPosition(sequenceLabel = "A2", capturedDateTime = LocalDateTime.of(2025, 1, 1, 2, 0))
              withPosition()
            }
          }
        }
      }

      val body = webTestClient.get()
        .uri("/crime-versions/$versionId")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING_GENERAL_RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-crime-version-matching-result-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.STRICT,
      )
    }

    @Test
    fun `it should return a crime version with a zero return matching result`() {
      val versionId = UUID.fromString("11111111-1111-1111-1111-111111111111")
      crimeMatchingFixtures.givenBatch(batchId = "Batch1") {
        withCrime("crime1", id = versionId) {
          withMatchingRun {}
        }
      }

      val body = webTestClient.get()
        .uri("/crime-versions/$versionId")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING_GENERAL_RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-crime-version-zero-return-matching-result-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.STRICT,
      )
    }
  }
}
