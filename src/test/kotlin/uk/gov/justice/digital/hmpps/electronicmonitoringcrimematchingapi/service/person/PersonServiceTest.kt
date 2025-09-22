package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.DeviceActivation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person.PersonRepository
import java.time.LocalDateTime

@ActiveProfiles("test")
class PersonServiceTest {
  private lateinit var personRepository: PersonRepository
  private lateinit var service: PersonService

  @BeforeEach
  fun setup() {
    personRepository = Mockito.mock(PersonRepository::class.java)
    service = PersonService(personRepository)
  }

  @Nested
  @DisplayName("GetPersons")
  inner class GetPersons {
    @Test
    fun `it should return a list of persons without device activations when includeDeviceActivations is false`() {
      val personsQueryCriteria = PersonsQueryCriteria(name = "name", includeDeviceActivations = false)

      val expectedResult = listOf(
        Person(
          personId = 1,
          personName = "name",
          nomisId = "nomisId",
          pncRef = "",
          probationPractitioner = "",
          dob = "1990-01-01",
          zip = "FK12 3FA",
          city = "Fakesville",
          street = "123 Fake Street",
          deviceActivations = mutableListOf(),
        ),
      )

      whenever(personRepository.getPersons(personsQueryCriteria)).thenReturn(expectedResult)

      val result = service.getPersons(personsQueryCriteria)

      assertThat(result).isInstanceOf(List::class.java)
      assertThat(result.count()).isEqualTo(1)
      assertThat(result.first()).isInstanceOf(Person::class.java)
      assertThat(result.first().deviceActivations).isEqualTo(listOf<DeviceActivation>())
    }

    @Test
    fun `it should return a list of persons with device activations when includeDeviceActivations is true`() {
      val personsQueryCriteria = PersonsQueryCriteria(name = "name", includeDeviceActivations = true)

      val expectedResult = listOf(
        Person(
          personId = 1,
          personName = "name",
          nomisId = "nomisId",
          pncRef = "",
          probationPractitioner = "",
          dob = "1990-01-01",
          zip = "FK12 3FA",
          city = "Fakesville",
          street = "123 Fake Street",
          deviceActivations = mutableListOf(
            DeviceActivation(
              deviceActivationId = 54321,
              deviceId = 12345,
              deviceName = "",
              personId = 1,
              deviceActivationDate = LocalDateTime.of(2021, 1, 1, 1, 1),
              deviceDeactivationDate = null,
              orderStart = "",
              orderEnd = "",
            ),
          ),
        ),
      )

      whenever(personRepository.getPersons(personsQueryCriteria)).thenReturn(expectedResult)

      val result = service.getPersons(personsQueryCriteria)

      assertThat(result).isInstanceOf(List::class.java)
      assertThat(result.count()).isEqualTo(1)
      assertThat(result.first()).isInstanceOf(Person::class.java)
      assertThat(result.first().deviceActivations).hasSize(1)
    }
  }
}
