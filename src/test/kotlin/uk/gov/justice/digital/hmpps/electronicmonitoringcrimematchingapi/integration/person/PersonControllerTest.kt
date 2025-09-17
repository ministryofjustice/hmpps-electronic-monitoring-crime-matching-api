package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.PaginatedResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonDto

@ActiveProfiles("integration")
class PersonControllerTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /persons")
  inner class GetPersons {
    @Test
    fun `it should return persons without device activations`() {
      stubQueryExecution(
        "123",
        "SUCCEEDED",
        "athenaResponses/successfulPersonsResponse.json",
      )

      val result = webTestClient.get()
        .uri("/persons?name=name")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList(PaginatedResponse::class.java)
        .hasSize(1)
        .returnResult()
        .responseBody!!

      assertThat(result[0].data).isNotNull()
      assertThat(result[0].data).isNotEmpty()
      assertThat(result[0].data[0]).extracting("deviceActivations").isNull()
    }

    @Test
    fun `it should return persons with device activations`() {
      stubQueryExecution(
        "123",
        "SUCCEEDED",
        "athenaResponses/successfulPersonsResponse.json",
      )

      val result = webTestClient.get()
        .uri("/persons?name=name&includeDeviceActivations=true")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList(PaginatedResponse::class.java)
        .hasSize(1)
        .returnResult()
        .responseBody!!

      assertThat(result[0].data).isNotNull()
      assertThat(result[0].data).isNotEmpty()
      assertThat(result[0].data[0]).extracting("deviceActivations").isNotNull()
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
      assertThat(result.name).isEqualTo("person_name")
      assertThat(result.address).isEqualTo("street city zip")
      assertThat(result.dateOfBirth).isEqualTo("2000-05-29")
      assertThat(result.deviceActivations).isNotNull().isEmpty()
    }
  }
}
