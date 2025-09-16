package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.entity.person.PersonsQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.caching.CacheEntryRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person.PersonsQueryCacheRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.ZonedDateTime

@ActiveProfiles("integration")
class PersonControllerTest : IntegrationTestBase() {

  @Autowired
  lateinit var personsQueryCacheRepository: PersonsQueryCacheRepository

  @Autowired
  lateinit var cacheEntryRepository: CacheEntryRepository

  @BeforeEach
  fun setup() {
    personsQueryCacheRepository.deleteAll()
    cacheEntryRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /persons")
  inner class GetPersons {
    @Test
    fun `it should return persons without device activations and store query in cache`() {
      stubQueryExecution(
        "123",
        "SUCCEEDED",
        "athenaResponses/successfulPersonsResponse.json",
      )

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

      val databaseResult =
        personsQueryCacheRepository.findByNomisIdAndPersonNameAndDeviceIdAndIncludeDeviceActivationsAndCreatedAtAfter(
          null,
          "name",
          null,
          false,
          ZonedDateTime.now().minusDays(1),
        )
      assertThat(databaseResult).isNotNull()
    }

    @Test
    fun `it should return persons with device activations`() {
      stubQueryExecution(
        "123",
        "SUCCEEDED",
        "athenaResponses/successfulPersonsResponse.json",
      )

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
      stubQueryExecution(
        "123",
        "SUCCEEDED",
        "athenaResponses/successfulPersonsResponse.json",
      )

      personsQueryCacheRepository.save(
        PersonsQuery(
          personName = "name",
          nomisId = null,
          deviceId = null,
          includeDeviceActivations = false,
          queryExecutionId = "query-execution-id",
          queryOwner = "user",
        ),
      )

      webTestClient.get()
        .uri("/persons?personName=name")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList(PersonDto::class.java)
        .hasSize(1)

      val databaseResult =
        personsQueryCacheRepository.findByNomisIdAndPersonNameAndDeviceIdAndIncludeDeviceActivationsAndCreatedAtAfter(
          null,
          "name",
          null,
          false,
          ZonedDateTime.now().minusDays(1),
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

    @Test
    fun `it should return an INTERNAL_SERVER_ERROR response if the Athena query fails`() {
      stubQueryExecution(
        "456",
        "FAILED",
        "athenaResponses/successfulPersonsResponse.json",
      )

      val response = webTestClient.get()
        .uri("/persons?personName=name")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody(ErrorResponse::class.java)
        .returnResult()
        .responseBody!!

      assertThat(response).isEqualTo(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: There was an unexpected error processing the request.",
          developerMessage = "There was an unexpected error processing the request.",
        ),
      )
    }
  }

  @Nested
  @DisplayName("GET /persons/{personId}")
  inner class GetPerson {
    @Test
    fun `it should return a NOT_FOUND response if person was not found in Athena`() {
      stubQueryExecution(
        "123",
        "SUCCEEDED",
        "athenaResponses/successfulEmptyPersonResponse.json",
      )

      webTestClient.get()
        .uri("/persons/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `it should return an OK response if person was found in Athena`() {
      stubQueryExecution(
        "123",
        "SUCCEEDED",
        "athenaResponses/successfulPersonsResponse.json",
      )

      val result = webTestClient.get()
        .uri("/persons/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(PersonDto::class.java)
        .returnResult()
        .responseBody!!

      assertThat(result.personId).isEqualTo("1")
      assertThat(result.nomisId).isEqualTo("nomis_id")
      assertThat(result.personName).isEqualTo("person_name")
      assertThat(result.address).isEqualTo("street city zip")
      assertThat(result.dateOfBirth).isEqualTo("2000-05-29")
      assertThat(result.deviceActivations).isNotNull().isEmpty()
    }

    @Test
    fun `it should use the cached query execution when a duplicate request is made`() {
      stubQueryExecution(
        "123",
        "SUCCEEDED",
        "athenaResponses/successfulPersonsResponse.json",
      )

      webTestClient.get()
        .uri("/persons/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk

      webTestClient.get()
        .uri("/persons/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk

      // Only one query should have been started
      verifyAthenaStartQueryExecutionCount(1)
      // The status of the existing query should have been checked twice
      verifyAthenaGetQueryExecutionCount(2)
      // The results of the existing query should have been used twice
      verifyAthenaGetQueryResultsCount(2)
    }

    @Test
    fun `it should return an INTERNAL_SERVER_ERROR response if the Athena query fails`() {
      stubQueryExecution(
        "456",
        "FAILED",
        "athenaResponses/successfulPersonsResponse.json",
      )

      val response = webTestClient.get()
        .uri("/persons/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody(ErrorResponse::class.java)
        .returnResult()
        .responseBody!!

      assertThat(response).isEqualTo(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: There was an unexpected error processing the request.",
          developerMessage = "There was an unexpected error processing the request.",
        ),
      )
    }
  }
}
