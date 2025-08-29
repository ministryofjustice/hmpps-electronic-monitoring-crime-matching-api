package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.entity.person.PersonsQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.mocks.MockEmDatastoreClient
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person.PersonsQueryCacheRepository
import java.time.ZonedDateTime

@ActiveProfiles("integration")
class PersonControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var personsQueryCacheRepository: PersonsQueryCacheRepository

  @BeforeEach
  fun setup() {
    personsQueryCacheRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /persons")
  inner class GetPersons {
    @Test
    fun `it should return persons without device activations and store query in cache`() {
      MockEmDatastoreClient.addResponseFile("successfulPersonsResponse")
      MockEmDatastoreClient.addResponseFile("successfulGetQueryExecutionIdResponse")

      val result = webTestClient.get()
        .uri("/persons?personName=name")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList(PersonDto::class.java)
        .hasSize(1)
        .returnResult()
        .responseBody!!

      assertThat(result[0].deviceActivations).isNull()

      val databaseResult = personsQueryCacheRepository.findByNomisIdAndPersonNameAndDeviceIdAndIncludeDeviceActivationsAndCreatedAtAfter(
        null,
        "name",
        null,
        false,
        ZonedDateTime.now().minusDays(1)
      )
      assertThat(databaseResult).isNotNull()
    }
  }

  @Test
  fun `it should return persons with device activations`() {
    MockEmDatastoreClient.addResponseFile("successfulPersonsResponse")
    MockEmDatastoreClient.addResponseFile("successfulGetQueryExecutionIdResponse")

    val result = webTestClient.get()
      .uri("/persons?personName=name&includeDeviceActivations=true")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(PersonDto::class.java)
      .hasSize(1)
      .returnResult()
      .responseBody!!

    assertThat(result[0].deviceActivations).isNotNull()
  }

  @Test
  fun `it should return persons and reuse existing query when found in cache`() {
    MockEmDatastoreClient.addResponseFile("successfulPersonsResponse")

    personsQueryCacheRepository.save(
      PersonsQuery(
        personName = "name",
        nomisId = null,
        deviceId = null,
        includeDeviceActivations = false,
        queryExecutionId = "query-execution-id",
        queryOwner = "user",
      )
    )

    webTestClient.get()
      .uri("/persons?personName=name")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(PersonDto::class.java)
      .hasSize(1)

    val databaseResult = personsQueryCacheRepository.findByNomisIdAndPersonNameAndDeviceIdAndIncludeDeviceActivationsAndCreatedAtAfter(
      null,
      "name",
      null,
      false,
      ZonedDateTime.now().minusDays(1)
    )
    assertThat(databaseResult).isNotNull()
    assertThat(databaseResult?.queryExecutionId).isEqualTo("query-execution-id")
  }

  @Test
  fun `it should fail with bad request when invalid criteria fields are passed`() {
    webTestClient.get()
      .uri("/persons")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus()
      .isBadRequest
  }
}