package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.person.PersonService

@ActiveProfiles("test")
class PersonControllerTest {
  private lateinit var service: PersonService
  private lateinit var controller: PersonController

  @BeforeEach
  fun setup() {
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
          name = "name",
          nomisId = "nomis",
          pncRef = "",
          dateOfBirth = "1990-01-01",
          probationPractitioner = "",
          address = "street city zip",
          deviceActivations = listOf(),
        ),
      )

      whenever(service.getPersons(personsQueryCriteria)).thenReturn(
        listOf(
          Person(
            personId = 1,
            personName = "name",
            nomisId = "nomis",
            pncRef = "",
            dob = "1990-01-01",
            probationPractitioner = "",
            zip = "zip",
            city = "city",
            street = "street",
            deviceActivations = mutableListOf(),
          ),
        ),
      )

      val result = controller.getPersons(personsQueryCriteria)
      assertThat(result.body).isNotNull()
      assertThat(result.body?.data).isNotNull()
      assertThat(result.body?.data).isEqualTo(expectedResult)
    }

    @Test
    fun `it should throw an exception when no valid criteria fields are passed`() {
      val personsQueryCriteria = PersonsQueryCriteria()

      assertThrows<ResponseStatusException> {
        controller.getPersons(personsQueryCriteria)
      }
    }

    @Test
    fun `it should throw an exception when includeDeviceActivations is false and deviceId is present`() {
      val personsQueryCriteria = PersonsQueryCriteria(deviceId = "deviceId")

      assertThrows<ResponseStatusException> {
        controller.getPersons(personsQueryCriteria)
      }
    }
  }
}
