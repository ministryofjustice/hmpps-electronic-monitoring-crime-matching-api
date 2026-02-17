package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PagedResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.Response
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@ActiveProfiles("integration")
class PersonControllerTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /persons")
  inner class GetPersons {
    @Test
    fun `it should return persons without device activations`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/successfulPersonsResponse.json",
      )

      val result = webTestClient.get()
        .uri("/persons?name=name")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PagedResponse<PersonResponse>>()
        .returnResult()
        .responseBody!!

      assertThat(result.data).isNotNull()
      assertThat(result.data).hasSize(1)
      assertThat(result.data[0].deviceActivations).isEmpty()
    }

    @Test
    fun `it should return persons with device activations`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/successfulPersonsResponseWithDeviceActivations.json",
      )

      val result = webTestClient.get()
        .uri("/persons?name=name&includeDeviceActivations=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PagedResponse<PersonResponse>>()
        .returnResult()
        .responseBody!!

      assertThat(result.data).isNotNull()
      assertThat(result.data).hasSize(1)
      assertThat(result.data[0].deviceActivations).hasSize(1)
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
      stubFailedQueryExecution("123")

      val response = webTestClient.get()
        .uri("/persons?name=name")
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
        1,
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
        1,
        "SUCCEEDED",
        "athenaResponses/successfulPersonsResponse.json",
      )

      val result = webTestClient.get()
        .uri("/persons/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<Response<PersonResponse>>()
        .returnResult()
        .responseBody!!

      assertThat(result.data).isEqualTo(
        PersonResponse(
          personId = 1,
          name = "person_name",
          nomisId = "nomis_id",
          pncRef = "",
          dateOfBirth = "2000-05-29",
          probationPractitioner = "",
          address = "street city zip",
          deviceActivations = listOf(),
        ),
      )
    }

    @Test
    fun `it should use the cached query execution when a duplicate request is made`() {
      stubQueryExecution(
        "123",
        1,
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
      stubFailedQueryExecution("123")

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

    @Test
    fun `it should keep retrying to get query results until the query is finished`() {
      stubQueryExecution(
        "123",
        3,
        "SUCCEEDED",
        "athenaResponses/successfulPersonsResponse.json",
      )

      webTestClient.get()
        .uri("/persons/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk

      // Only one query should have been started
      verifyAthenaStartQueryExecutionCount(1)
      // The status of the existing query should have been checked twice
      verifyAthenaGetQueryExecutionCount(3)
      // The results of the existing query should have been used twice
      verifyAthenaGetQueryResultsCount(1)
    }
  }
}
