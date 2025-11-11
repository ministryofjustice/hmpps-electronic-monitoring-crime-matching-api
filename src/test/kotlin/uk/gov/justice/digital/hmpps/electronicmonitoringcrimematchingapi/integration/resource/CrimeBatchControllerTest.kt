package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.json.JsonCompareMode
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.GeodeticDatum
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import java.time.LocalDateTime

@ActiveProfiles("integration")
class CrimeBatchControllerTest : IntegrationTestBase() {
  @Autowired
  lateinit var repo: CrimeBatchRepository

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
        .uri("/crime-batches/1")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should return a 404 if the crime batch does not exist`() {
      webTestClient.get()
        .uri("/crime-batches/1")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO")))
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `it should return a crime batch if it exists`() {
      // Create a crime batch
      val id = "142a9a57-337f-4208-908b-2874b75fa10d"
      val batch = CrimeBatch(
        id = id,
        policeForce = PoliceForce.METROPOLITAN,
      )
      val crimes = mutableListOf(
        Crime(
          crimeTypeId = CrimeType.AB,
          crimeReference = "CRI00000001",
          crimeDateTimeFrom = LocalDateTime.of(2025, 1, 25, 8, 30),
          crimeDateTimeTo =  LocalDateTime.of(2025, 1, 25, 8, 30),
          easting = null,
          northing = null,
          latitude = 51.574865,
          longitude = 0.060977,
          datum = GeodeticDatum.WGS84,
          crimeText = "",
          crimeBatch = batch,
        )
      )
      batch.crimes.addAll(crimes)
      repo.save(batch)

      // Validate crime batch is retrieved successfully
      webTestClient.get()
        .uri("/crime-batches/$id")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CRIME_BATCHES__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("get-crime-batch-response".loadJson(), JsonCompareMode.STRICT)
    }
  }

  private fun String.loadJson(): String = CrimeBatchControllerTest::class.java.getResource("$this.json")!!.readText()
}
