package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.http.MediaType
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
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingRunRepository
import java.time.Instant
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

@ActiveProfiles("integration")
class CrimeMatchingRunControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var crimeMatchingRunRepository: CrimeMatchingRunRepository

  @Autowired
  lateinit var crimeBatchRepository: CrimeBatchRepository

  @Autowired
  lateinit var crimeRepository: CrimeRepository

  @Autowired
  lateinit var crimeVersionRepository: CrimeVersionRepository

  @Autowired
  lateinit var crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository

  @Nested
  @DisplayName("POST /crime-matching-run")
  inner class CreateCrimeMatchingRun {

    @Test
    fun `it should return a 401 if the request is not authenticated`() {
      webTestClient.post()
        .uri("/crime-matching-run")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("create-crime-matching-run-request".loadJson())
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

   @Test
    fun `it should return a 403 if the client does not have a valid role`() {
      webTestClient.post()
        .uri("/crime-matching-run")
        .headers(setAuthorisation(roles = listOf()))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("create-crime-matching-run-request".loadJson())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should return a 404 if the crime batch does not exist`() {
      webTestClient.post()
        .uri("/crime-matching-run")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_MATCHING_RESULTS__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("create-crime-matching-run-request-missing-batch".loadJson())
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("$.developerMessage")
        .isEqualTo("No crime batch found with id: 00000000-0000-0000-0000-000000000001")
        .jsonPath("$.userMessage").isEqualTo("Not Found")
    }

    @Test
    fun `it should return a 404 if the crime version does not exist`() {
      createCrimeBatchWithCrimeVersion()

      webTestClient.post()
        .uri("/crime-matching-run")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_MATCHING_RESULTS__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("create-crime-matching-run-request-missing-crime-version".loadJson())
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("$.developerMessage")
        .isEqualTo("No crime version found with id: 00000000-0000-0000-0000-000000000001")
        .jsonPath("$.userMessage").isEqualTo("Not Found")
    }

    @Test
    fun `it should create a crime matching run and return 201 with run id`() {
      val (crimeBatchId, _) = createCrimeBatchWithCrimeVersion()

      webTestClient.post()
        .uri("/crime-matching-run")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_MATCHING_RESULTS__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("create-crime-matching-run-request".loadJson())
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.data.id").exists()
        .jsonPath("$.data.id").isNotEmpty

      assertThat(crimeMatchingRunRepository.count()).isEqualTo(1)

      val run = crimeMatchingRunRepository.findAll().first()
      assertThat(run.algorithmVersion).isEqualTo("e83c5163316f89bfbde7d9ab23ca2e25604af290")
      assertThat(run.crimeBatch.id).isEqualTo(crimeBatchId)
      assertThat(run.triggerType.name).isEqualTo("AUTO")
      assertThat(run.status.name).isEqualTo("SUCCESS")
    }
  }

  // Creates a CrimeBatch, plus a Crime + CrimeVersion, and returns their IDs.
  private fun createCrimeBatchWithCrimeVersion(): Pair<UUID, UUID> {
    val crimeBatchIngestionAttempt = CrimeBatchIngestionAttempt(
      bucket = "bucket",
      objectName = "objectName",
    )

    val crimeBatchEmail = CrimeBatchEmail(
      crimeBatchIngestionAttempt = crimeBatchIngestionAttempt,
      sender = "sender",
      originalSender = "originalSender",
      subject = "subject",
      sentAt = Date.from(Instant.now()),
    )

    val crimeBatchEmailAttachment = CrimeBatchEmailAttachment(
      crimeBatchEmail = crimeBatchEmail,
      fileName = "filename",
      rowCount = 1,
    )

    crimeBatchEmail.crimeBatchEmailAttachments.add(crimeBatchEmailAttachment)
    crimeBatchIngestionAttempt.crimeBatchEmail = crimeBatchEmail
    crimeBatchIngestionAttemptRepository.save(crimeBatchIngestionAttempt)

    val crimeBatchId = UUID.fromString("242a9a57-337f-4208-908b-2874b75fa10e")
    val crimeBatch = CrimeBatch(
      id = crimeBatchId,
      batchId = "batchId",
      crimeBatchEmailAttachment = crimeBatchEmailAttachment,
    )

    val crime = crimeRepository.save(
      Crime(
        policeForceArea = PoliceForce.METROPOLITAN,
        crimeReference = "CRI-${UUID.randomUUID()}",
      ),
    )

    val crimeVersionId = UUID.fromString("252a9a57-337f-4208-908b-2874b75fa10f")
    val version = crimeVersionRepository.save(
      CrimeVersion(
        id = crimeVersionId,
        crime = crime,
        crimeTypeId = CrimeType.AB,
        crimeDateTimeFrom = LocalDateTime.of(2025, 1, 25, 8, 30),
        crimeDateTimeTo = LocalDateTime.of(2025, 1, 25, 8, 30),
        easting = null,
        northing = null,
        latitude = 51.574865,
        longitude = 0.060977,
        crimeText = "",
      ),
    )

    crimeBatch.crimeVersions.add(version)
    crimeBatchRepository.save(crimeBatch)

    return Pair(crimeBatchId, crimeVersionId)
  }

  private fun String.loadJson(): String = CrimeMatchingRunControllerTest::class.java.getResource("$this.json")!!.readText()
}
