package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeBatchBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeBatchEmailAttachmentBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeBatchEmailBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeBatchIngestionAttemptBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeMatchingResultBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeMatchingResultDeviceWearerBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeMatchingResultPositionBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeMatchingRunBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.CrimeVersionBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultDeviceWearer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultPosition
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingRun
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeMatchingStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeMatchingTriggerType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingRunRepository
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

@ActiveProfiles("integration")
class CrimeBatchIngestionAttemptControllerTest: IntegrationTestBase() {
  @Autowired
  lateinit var repo: CrimeBatchIngestionAttemptRepository

  @Autowired
  lateinit var crimeRepository: CrimeRepository

  @Autowired
  lateinit var crimeVersionRepository: CrimeVersionRepository

  @Autowired
  lateinit var crimeBatchRepository: CrimeBatchRepository

  @Autowired
  lateinit var crimeMatchingRunRepository: CrimeMatchingRunRepository

  @BeforeEach
  fun setup() {
    repo.deleteAll()
  }

  @Nested
  @DisplayName("GET /ingestion-attempts")
  inner class GetIngestionAttempts {
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
    fun `it should return a list of ingestion attempts`() {
      // Create a crime batch
      val crimeBatchIngestionAttempt = createCrimeBatchWithCrimeVersion()

      val body = webTestClient.get()
        .uri("/ingestion-attempts")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCH_INGESTION_ATTEMPTS__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!
      val formatted = String(body, StandardCharsets.UTF_8)

      println("finished")
    }
  }

  // Creates a CrimeBatch, plus a Crime + CrimeVersion, and returns their IDs.
  private fun createCrimeBatchWithCrimeVersion(): CrimeBatchIngestionAttempt {
    // Create a crime batch
    val crimeBatchIngestionAttempt = CrimeBatchIngestionAttemptBuilder.aCrimeBatchIngestionAttempt()

    val crimeBatchEmail = CrimeBatchEmailBuilder.aCrimeBatchEmail(crimeBatchIngestionAttempt)

    val crimeBatchEmailAttachment = CrimeBatchEmailAttachmentBuilder.aCrimeBatchEmailAttachment(crimeBatchEmail)

    repo.save(crimeBatchIngestionAttempt)

    val crimeBatch = CrimeBatchBuilder.aCrimeBatch(crimeBatchEmailAttachment = crimeBatchEmailAttachment)

    val crime = CrimeBuilder.aCrime()
    crimeRepository.save(crime)

    val crimeVersionId = UUID.fromString("252a9a57-337f-4208-908b-2874b75fa10f")
    val crimeVersion = CrimeVersionBuilder.aCrimeVersion(
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
    )

    crimeVersionRepository.save(crimeVersion)
    crimeBatch.crimeVersions.add(crimeVersion)
    crimeBatchRepository.save(crimeBatch)

    // Matching results setup
    val crimeMatchingRun = CrimeMatchingRunBuilder.aCrimeMatchingRun()

    val crimeMatchingResult = CrimeMatchingResultBuilder.aCrimeMatchingResult(crimeMatchingRun = crimeMatchingRun, crimeVersion = crimeVersion)

    val deviceWearer = CrimeMatchingResultDeviceWearerBuilder.aCrimeMatchingResultDeviceWearer(crimeMatchingResult = crimeMatchingResult)

    val positions = mutableListOf(
      CrimeMatchingResultPositionBuilder.aCrimeMatchingResultPosition(deviceWearer = deviceWearer)
    )

    deviceWearer.positions = positions

    val deviceWearers = mutableListOf(
      deviceWearer
    )

    crimeMatchingResult.deviceWearers = deviceWearers

    val results = mutableListOf(
      crimeMatchingResult
    )

    crimeMatchingRun.results = results
    crimeMatchingRun.crimeBatch = crimeBatch

    crimeMatchingRunRepository.save(crimeMatchingRun)


    return crimeBatchIngestionAttempt
  }
}