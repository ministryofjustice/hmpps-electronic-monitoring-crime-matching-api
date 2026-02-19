package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeBatchBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeBatchEmailAttachmentBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeBatchEmailBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeBatchIngestionAttemptBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeVersionBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

@ActiveProfiles("integration")
class CrimeBatchControllerTest : IntegrationTestBase() {
  @Autowired
  lateinit var repo: CrimeBatchRepository

  @Autowired
  lateinit var crimeRepository: CrimeRepository

  @Autowired
  lateinit var crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository

  @BeforeEach
  fun setup() {
    repo.deleteAll()
  }

  @Nested
  @DisplayName("GET /crime-batches/{crimeBatchId}")
  inner class GetCrimeBatch {
    @Test
    fun `it should return a 401 if the request is not authenticated`() {
      webTestClient.get()
        .uri("/crime-batches/1")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return a 403 if the client does not have a valid role`() {
      webTestClient.get()
        .uri("/crime-batches/" + UUID.randomUUID())
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should return a 404 if the crime batch does not exist`() {
      val id = UUID.randomUUID()
      webTestClient.get()
        .uri("/crime-batches/$id")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO")))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("$.developerMessage").isEqualTo("No crime batch found with id: $id")
        .jsonPath("$.userMessage").isEqualTo("Not Found")
    }

    @Test
    fun `it should return a crime batch if it exists`() {
      // Create a crime batch
      val crimeBatchIngestionAttempt = CrimeBatchIngestionAttemptBuilder.aCrimeBatchIngestionAttempt()

      val crimeBatchEmail = CrimeBatchEmailBuilder.aCrimeBatchEmail(crimeBatchIngestionAttempt)

      val crimeBatchEmailAttachment = CrimeBatchEmailAttachmentBuilder.aCrimeBatchEmailAttachment(crimeBatchEmail)

      crimeBatchIngestionAttemptRepository.save(crimeBatchIngestionAttempt)

      val crimeBatch = CrimeBatchBuilder.aCrimeBatch(crimeBatchEmailAttachment = crimeBatchEmailAttachment)

      val crime = CrimeBuilder.aCrime()
      crimeRepository.save(crime)

      val crimeVersions = mutableListOf(
        CrimeVersionBuilder.aCrimeVersion(
          id = UUID.fromString("152a9a57-337f-4208-908b-2874b75fa10e"),
          crime = crime,
          latitude = 51.574865,
          longitude = 0.060977,
        ),
        CrimeVersionBuilder.aCrimeVersion(
          id = UUID.fromString("8d595ab3-d4ef-4975-93ef-24570f6f0f61"),
          crime = crime,
          crimeTypeId = CrimeType.BOTD,
          crimeDateTimeFrom = LocalDateTime.of(2025, 1, 1, 0, 30),
          crimeDateTimeTo = LocalDateTime.of(2025, 1, 1, 1, 30),
          easting = 529381.toDouble(),
          northing = 179534.toDouble(),
        ),
      )
      crimeBatch.crimeVersions.addAll(crimeVersions)
      repo.save(crimeBatch)

      // Validate crime batch is retrieved successfully
      val body = webTestClient.get()
        .uri("/crime-batches/${crimeBatch.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-crime-batch-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }
  }

  private fun String.loadJson(): String = CrimeBatchControllerTest::class.java.getResource("$this.json")!!.readText()
}
