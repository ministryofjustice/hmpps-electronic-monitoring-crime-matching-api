package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles("integration")
class ManagementInformationControllerTest : IntegrationTestBase() {
  private val objectMapper: ObjectMapper = jacksonObjectMapper()

  @Nested
  @DisplayName("GET /management-information/zero-precision-crime-matches")
  inner class GetZeroPrecisionCrimeMatches {
    private val path = "/management-information/zero-precision-crime-matches"
    private val pagedPath = "$path?page=0"

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
        .uri(pagedPath)
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
        .uri(pagedPath)
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
        .uri(pagedPath)
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
              "policeForceArea": "Metropolitan",
              "batchId": "Batch1",
              "crimeReference": "crime1",
              "deviceSerialNumber": "000000002",
              "matchedAt": "2025-01-01T01:00:00Z",
              "zeroPrecisionPositionCount": 1
            },
            {
              "policeForceArea": "Metropolitan",
              "batchId": "Batch1",
              "crimeReference": "crime1",
              "deviceSerialNumber": "000000001",
              "matchedAt": "2025-01-01T01:00:00Z",
              "zeroPrecisionPositionCount": 2
            },
            {
              "policeForceArea": "Metropolitan",
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

    @Test
    fun `it should return every zero-precision crime match if page is omitted`() {
      // Given 11 matched device wearers with zero-precision positions
      givenCrimeMatches(11)

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

      val response = objectMapper.readTree(body)

      // Then all 11 matches should be returned
      assertThat(response["data"].size()).isEqualTo(11)
    }

    @Test
    fun `it should return the first 10 zero-precision crime matches if page is included`() {
      // Given 11 matched device wearers with zero-precision positions
      givenCrimeMatches(11)

      // When we request zero-precision matches
      val body = webTestClient.get()
        .uri(pagedPath)
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      val response = objectMapper.readTree(body)

      // Then the first 10 matches should be returned
      assertThat(response["data"].size()).isEqualTo(10)
    }

    @Test
    fun `it should return the second page of zero-precision crime matches`() {
      // Given 11 matched device wearers with zero-precision positions
      givenCrimeMatches(11)

      // When we request zero-precision matches
      val body = webTestClient.get()
        .uri("$path?page=1")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      val response = objectMapper.readTree(body)

      // Then the second page of matches should be returned
      assertThat(response["data"].size()).isEqualTo(1)
    }
  }

  @Nested
  @DisplayName("GET /management-information/failed-crime-submissions")
  inner class GetFailedCrimeSubmissions {
    private val path = "/management-information/failed-crime-submissions"

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
        .uri("$path?policeForceArea=UNKNOWN")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should return 400 if the police force is missing`() {
      webTestClient.get()
        .uri(path)
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @ParameterizedTest
    @ValueSource(
      strings = [
        "?policeForceArea=INVALID",
        "?policeForceArea=UNKNOWN&from=invalid",
        "?policeForceArea=UNKNOWN&to=invalid",
      ],
    )
    fun `it should return 400 if the request filters are invalid`(query: String) {
      webTestClient.get()
        .uri("$path$query")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `it should return all error types with zero counts when an attachment has no errors`() {
      // Given an ingestion attempt with no errors exists
      crimeMatchingFixtures.givenIngestionAttempt {
        withAttachment()
      }

      // When we request failed crime submissions
      val body = webTestClient.get()
        .uri("$path?policeForceArea=UNKNOWN")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      // Then the response should contain a 0 total count and all error types with zero counts
      assertFailedCrimeSubmissionsResponse(body, total = 0)
    }

    @Test
    fun `it should count a row with multiple errors only once`() {
      // Given one row with multiple errors and another row with one error
      crimeMatchingFixtures.givenIngestionAttempt {
        withAttachment(rowCount = 2) {
          withAttachmentIngestionError(errorType = CrimeBatchEmailAttachmentIngestionErrorType.MISSING_CRIME_REFERENCE)
          withAttachmentIngestionError(errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_TEXT)
          withAttachmentIngestionError(errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_NUMBER)
          withAttachmentIngestionError(rowNumber = 2, errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_NUMBER)
        }
      }

      // When we request failed crime submissions
      val body = webTestClient.get()
        .uri("$path?policeForceArea=UNKNOWN")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      // Then the errors should be aggregated without counting a row more than once
      assertFailedCrimeSubmissionsResponse(
        body,
        total = 2,
        counts = mapOf(CrimeBatchEmailAttachmentIngestionErrorType.INVALID_NUMBER to 2),
      )
    }

    @Test
    fun `it should count errors independently across attachments`() {
      // Given three attachments with errors on row one in separate ingestion attempts
      repeat(3) {
        crimeMatchingFixtures.givenIngestionAttempt {
          withAttachment {
            withAttachmentIngestionError()
          }
        }
      }

      // When we request failed crime submissions
      val body = webTestClient.get()
        .uri("$path?policeForceArea=UNKNOWN")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      // Then the response should contain a 3 total count and the missing crime reference error with a count of 3
      assertFailedCrimeSubmissionsResponse(
        body,
        total = 3,
        counts = mapOf(CrimeBatchEmailAttachmentIngestionErrorType.MISSING_CRIME_REFERENCE to 3),
      )
    }

    @Test
    fun `it should return counts for every error type when each occurs on a different row`() {
      // Given a separate failed row for every supported error type
      crimeMatchingFixtures.givenIngestionAttempt {
        withAttachment(rowCount = CrimeBatchEmailAttachmentIngestionErrorType.entries.size) {
          CrimeBatchEmailAttachmentIngestionErrorType.entries.forEachIndexed { index, type ->
            withAttachmentIngestionError(rowNumber = index.toLong() + 1, errorType = type)
          }
        }
      }

      // When we request failed crime submissions
      val body = webTestClient.get()
        .uri("$path?policeForceArea=UNKNOWN")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      // Then the response should contain a count of 1 for all errors and a total count matching the possible error types.
      assertFailedCrimeSubmissionsResponse(
        body,
        total = CrimeBatchEmailAttachmentIngestionErrorType.entries.size,
        counts = CrimeBatchEmailAttachmentIngestionErrorType.entries.associateWith { 1 },
      )
    }

    @Test
    fun `it should only count errors belonging to the requested police force`() {
      // Given failed rows for different police forces
      crimeMatchingFixtures.givenIngestionAttempt {
        withAttachment {
          withAttachmentIngestionError(policeForceArea = PoliceForce.METROPOLITAN)
        }
      }
      crimeMatchingFixtures.givenIngestionAttempt {
        withAttachment {
          withAttachmentIngestionError(policeForceArea = PoliceForce.ESSEX)
        }
      }

      // When we request failed crime submissions
      val body = webTestClient.get()
        .uri("$path?policeForceArea=METROPOLITAN")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      // Then the response should contain a 1 total count with 1 crime reference error count for the selected police force
      assertFailedCrimeSubmissionsResponse(
        body,
        total = 1,
        counts = mapOf(CrimeBatchEmailAttachmentIngestionErrorType.MISSING_CRIME_REFERENCE to 1),
      )
    }

    @Test
    fun `it should return zero counts when only another police force has errors`() {
      // Given a failed row
      crimeMatchingFixtures.givenIngestionAttempt {
        withAttachment {
          withAttachmentIngestionError(policeForceArea = PoliceForce.ESSEX)
        }
      }

      // When we request failed crime submissions for another police force
      val body = webTestClient.get()
        .uri("$path?policeForceArea=METROPOLITAN")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      // Then the response should contain a 0 total count and all error types with zero counts
      assertFailedCrimeSubmissionsResponse(body, total = 0)
    }

    @ParameterizedTest
    @CsvSource(
      "&from=2025-01-01T00:00:00Z, 3",
      "&to=2025-01-03T00:00:00Z, 3",
      "&from=2025-01-01T00:00:00Z&to=2025-01-03T00:00:00Z, 2",
      "&from=2025-01-04T00:00:00Z, 0",
    )
    fun `it should filter by ingestion creation date including from and excluding to`(query: String, expectedCount: Int) {
      // Given attempts before, at, between and at the end of the requested date range
      listOf("2024-12-31T00:00:00", "2025-01-01T00:00:00", "2025-01-02T00:00:00", "2025-01-03T00:00:00").forEach { createdAt ->
        crimeMatchingFixtures.givenIngestionAttempt(createdAt = LocalDateTime.parse(createdAt)) {
          withAttachment {
            withAttachmentIngestionError()
          }
        }
      }

      // When we request failed crime submissions
      val body = webTestClient.get()
        .uri("$path?policeForceArea=UNKNOWN$query")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      // Then the response should contain the expected count of failed rows
      assertFailedCrimeSubmissionsResponse(
        body,
        total = expectedCount,
        counts = mapOf(CrimeBatchEmailAttachmentIngestionErrorType.MISSING_CRIME_REFERENCE to expectedCount),
      )
    }

    private fun assertFailedCrimeSubmissionsResponse(
      body: ByteArray,
      total: Int,
      counts: Map<CrimeBatchEmailAttachmentIngestionErrorType, Int> = emptyMap(),
    ) {
      val expected = mapOf(
        "total" to total,
        "failures" to CrimeBatchEmailAttachmentIngestionErrorType.entries.map { type ->
          mapOf(
            "reasonForFailure" to type.message,
            "failureCount" to (counts[type] ?: 0),
          )
        },
      )

      val expectedResponse = mapOf(
        "data" to listOf(expected),
        "pageCount" to 1,
        "pageNumber" to 0,
        "pageSize" to counts.values.count { it > 0 },
      )

      JSONAssert.assertEquals(
        objectMapper.writeValueAsString(expectedResponse),
        body.toString(StandardCharsets.UTF_8),
        JSONCompareMode.STRICT,
      )
    }
  }

  @Nested
  @DisplayName("GET /management-information/crimes-submission-summary")
  inner class GetCrimeSubmissionSummary {
    private val path = "/management-information/crimes-submission-summary"

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
    fun `it returns every crime type and Missing when no submissions exist`() {
      val response = getSummary()

      val expectedTypes = CrimeType.entries.map { it.value } + "Missing"

      assertThat(response["data"].map { it["crimeType"].asText() })
        .isEqualTo(expectedTypes)

      response["data"].forEachIndexed { index, it ->
        assertCounts(it, expectedTypes[index], 0, 0, 0, 0.0)
      }

      assertThat(response["pageCount"].asInt()).isEqualTo(1)
      assertThat(response["pageNumber"].asInt()).isEqualTo(0)
      assertThat(response["pageSize"].asInt()).isEqualTo(expectedTypes.size)
    }

    @Test
    fun `it returns zero counts when the selected period has no submissions`() {
      // Given an ingestion attempt with a batch created before the requested period
      crimeMatchingFixtures.givenBatch(ingestionCreatedAt = LocalDateTime.parse("2025-01-01T00:00:00")) {
        withCrime("crime1") {
        }
      }

      // And another ingestion attempt with a batch created after the requested period
      crimeMatchingFixtures.givenBatch(ingestionCreatedAt = LocalDateTime.parse("2025-03-01T00:00:00")) {
        withCrime("crime1") {}
      }

      // When we request a summary for a period with no submissions
      val response = getSummary(
        from = "2025-02-01T00:00:00Z",
        to = "2025-02-28T00:00:00Z",
      )

      val expectedTypes = CrimeType.entries.map { it.value } + "Missing"

      // Then the response should contain zero counts for all crime types
      response["data"].forEachIndexed { index, nodes ->
        assertCounts(nodes, expectedTypes[index], 0, 0, 0, 0.0)
      }
    }

    @Test
    fun `it counts ingested crime within the selected period`() {
      // Given an ingestion attempt with a batch with ingested crimes
      crimeMatchingFixtures.givenBatch(ingestionCreatedAt = LocalDateTime.parse("2025-01-02T00:00:00")) {
        withCrime("crime1", crimeType = CrimeType.AB) {}
        withCrime("crime1", crimeType = CrimeType.BIAD) {}
        withCrime("crime1", crimeType = CrimeType.BOTD) {}
        withCrime("crime1", crimeType = CrimeType.RB) {}
        withCrime("crime1", crimeType = CrimeType.RB) {}
        withCrime("crime1", crimeType = CrimeType.TFMV) {}
        withCrime("crime1", crimeType = CrimeType.TFP) {}
        withCrime("crime1", crimeType = CrimeType.TOMV) {}
      }

      // And an ingestion attempt with a batch with some ingested crimes
      crimeMatchingFixtures.givenBatch(ingestionCreatedAt = LocalDateTime.parse("2025-01-02T00:00:00")) {
        withCrime("crime1", crimeType = CrimeType.BIAD) {}
      }

      // And an ingestion attempt with some failed crimes
      crimeMatchingFixtures.givenIngestionAttempt {
        withAttachment {
          withAttachmentIngestionError()
          withAttachmentIngestionError(crimeType = CrimeType.AB)
        }
      }

      // When we request a summary
      val response = getSummary(
        from = "2025-01-01T00:00:00Z",
        to = "2025-01-31T00:00:00Z",
      )

      // Then the response should contain zero counts for all crime types
      assertCounts(response["data"][0], "Aggravated Burglary", 2, 1, 1, 50.0)
      assertCounts(response["data"][1], "Burglary in a dwelling", 2, 2, 0, 0.0)
      assertCounts(response["data"][2], "Burglary in a building other than a dwelling", 1, 1, 0, 0.0)
      assertCounts(response["data"][3], "Robbery", 2, 2, 0, 0.0)
      assertCounts(response["data"][4], "Theft from vehicle", 1, 1, 0, 0.0)
      assertCounts(response["data"][5], "Theft from the person of another", 1, 1, 0, 0.0)
      assertCounts(response["data"][6], "Theft of a vehicle", 1, 1, 0, 0.0)
      assertCounts(response["data"][7], "Missing", 1, 0, 1, 100.0)
    }

    private fun getSummary(
      from: String? = null,
      to: String? = null,
      policeForceArea: String? = null,
    ): JsonNode {
      val body = webTestClient.get()
        .uri { builder ->
          builder.path(path)
          from?.let { builder.queryParam("from", it) }
          to?.let { builder.queryParam("to", it) }
          policeForceArea?.let { builder.queryParam("policeForceArea", it) }
          builder.build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__MI__RO")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      return objectMapper.readTree(body)
    }

    private fun assertCounts(
      row: JsonNode,
      crimeType: String,
      submitted: Long,
      ingested: Long,
      failed: Long,
      percentage: Double,
    ) {
      assertThat(row["crimeType"].asText()).isEqualTo(crimeType)
      assertThat(row["submitted"].asLong()).isEqualTo(submitted)
      assertThat(row["ingested"].asLong()).isEqualTo(ingested)
      assertThat(row["failed"].asLong()).isEqualTo(failed)
      assertThat(row["notIngestedPercentage"].asDouble()).isEqualTo(percentage)
    }
  }

  private fun givenCrimeMatches(count: Int) {
    crimeMatchingFixtures.givenBatch(batchId = "Batch1") {
      withCrime("crime1") {
        withMatchingRun {
          (1..count).forEach { deviceId ->
            withMatchedDeviceWearer(
              id = UUID.fromString("00000000-0000-0000-0000-${deviceId.toString().padStart(12, '0')}"),
              deviceId = deviceId.toLong(),
              deviceSerialNumber = deviceId.toString().padStart(9, '0'),
            ) {
              withPosition(precision = 0)
            }
          }
        }
      }
    }
  }
}
