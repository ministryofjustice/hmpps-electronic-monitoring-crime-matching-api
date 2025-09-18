package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.person.PersonService

@ActiveProfiles("test")
class PersonControllerTest {
  private lateinit var service: PersonService
  private lateinit var controller: PersonController
  private lateinit var authentication: Authentication

  @BeforeEach
  fun setup() {
    authentication = Mockito.mock(Authentication::class.java)
    whenever(authentication.name).thenReturn("MOCK_AUTH_USER")
    service = Mockito.mock(PersonService::class.java)
    controller = PersonController(service)
  }

  @Nested
  @DisplayName("GetPersons")
  inner class GetPersons {
    @Test
    fun `it should return persons when valid criteria passed`() {
      val personsQueryCriteria = PersonsQueryCriteria(name = "name")

      val expectedResult = listOf(
        PersonDto(
          1,
          "name",
          "nomisId",
          "pncRef",
          "1990-01-01",
          "probationPractitioner",
          "address",
          emptyList(),
        ),
      )

      whenever(service.getPersons(personsQueryCriteria, authentication.name)).thenReturn(expectedResult)

      val result = controller.getPersons(authentication, personsQueryCriteria)
      assertThat(result.body).isNotNull()
      assertThat(result.body?.data).isNotNull()
      assertThat(result.body?.data).isEqualTo(expectedResult)
    }

    @Test
    fun `it should throw an exception when no valid criteria fields are passed`() {
      val personsQueryCriteria = PersonsQueryCriteria()

      assertThrows<ResponseStatusException> {
        controller.getPersons(authentication, personsQueryCriteria)
      }
    }

    @Test
    fun `it should throw an exception when includeDeviceActivations is false and deviceId is present`() {
      val personsQueryCriteria = PersonsQueryCriteria(deviceId = "deviceId")

      assertThrows<ResponseStatusException> {
        controller.getPersons(authentication, personsQueryCriteria)
      }
    }
  }
}
