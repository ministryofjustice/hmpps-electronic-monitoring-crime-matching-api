package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PagedResult
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
    fun `it should return a list of persons with device activations`() {
      val personsQueryCriteria = PersonsQueryCriteria(name = "name")

      val expectedResult = PagedResult(
        data = listOf(
          Person(
            personId = "1",
            firstName = "firstName",
            lastName = "lastName",
            nomisId = "nomisId",
            pncRef = "pncId",
            probationPractitioner = "responsibleOfficerName",
            dateOfBirth = "1990-01-01",
            postcode = "FK12 3FA",
            cityOrTown = "Fakesville",
            street = "123 Fake Street",
            deviceActivations = mutableListOf(
              DeviceActivation(
                deviceActivationId = 54321,
                deviceId = 12345,
                deviceSerialNumber = "123456789",
                deviceName = "",
                uniqueDeviceWearerId = "1",
                deviceActivationDate = LocalDateTime.of(2021, 1, 1, 1, 1),
                deviceDeactivationDate = null,
                orderStart = "",
                orderEnd = "",
              ),
            ),
          ),
        ),
        pageCount = 1,
      )

      whenever(personRepository.getPersons(personsQueryCriteria, page = 1, pageSize = 10)).thenReturn(expectedResult)

      val result = service.getPersons(personsQueryCriteria, page = 1, pageSize = 10)

      assertThat(result).isInstanceOf(PagedResult::class.java)
      assertThat(result.data.count()).isEqualTo(1)
      assertThat(result.data.first()).isInstanceOf(Person::class.java)
      assertThat(result.data.first().deviceActivations).hasSize(1)
      assertThat(result.pageCount).isEqualTo(1)
    }
  }
}
