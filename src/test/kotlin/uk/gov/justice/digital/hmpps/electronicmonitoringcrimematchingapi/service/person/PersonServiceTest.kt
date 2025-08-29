package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.entity.person.PersonsQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaPersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person.PersonRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person.PersonsQueryCacheRepository
import java.time.ZonedDateTime
import java.util.UUID

@ActiveProfiles("test")
class PersonServiceTest {
  private lateinit var queryCacheRepository: PersonsQueryCacheRepository
  private lateinit var personRepository: PersonRepository
  private lateinit var service: PersonService

  @BeforeEach
  fun setup() {
    queryCacheRepository = Mockito.mock(PersonsQueryCacheRepository::class.java)
    personRepository = Mockito.mock(PersonRepository::class.java)
    service = PersonService(queryCacheRepository, personRepository)
  }

  @Nested
  @DisplayName("GetPersons")
  inner class GetPersons {
    @Test
    fun `it should return a list of persons without device activations when includeDeviceActivations is false`() {
      val personsQueryCriteria = PersonsQueryCriteria(personName = "name", includeDeviceActivations = false)

      val queryExecutionId = "query-execution-id"

      val expectedResult = listOf(
        AthenaPersonDto(
          personId = "personId",
          personName = "name",
          uIdNomis = "nomisId",
          uDob = "1990-01-01",
          zip = "FK12 3FA",
          city = "Fakesville",
          street = "123 Fake Street",
          deviceId = null,
          deviceActivationId = null,
          deviceActivationDate = null,
          deviceDeactivationDate = null,
        ),
      )

      whenever(
        queryCacheRepository.findByNomisIdAndPersonNameAndDeviceIdAndIncludeDeviceActivationsAndCreatedAtAfter(
          any(),
          eq("name"),
          any(),
          eq(false),
          any(),
        )
      ).thenReturn(null)

      whenever(personRepository.getPersonsQueryId(personsQueryCriteria)).thenReturn(queryExecutionId)
      whenever(personRepository.getPersonsQueryResults(queryExecutionId)).thenReturn(expectedResult)

      val result = service.getPersons(personsQueryCriteria, "")

      assertThat(result).isInstanceOf(List::class.java)
      assertThat(result.count()).isEqualTo(1)
      assertThat(result.first()).isInstanceOf(PersonDto::class.java)
      assertThat(result.first().deviceActivations).isNull()
      verify(queryCacheRepository, times(1))
        .save(any())
      verify(personRepository, times(1))
        .getPersonsQueryId(any())
    }

    @Test
    fun `it should return a list of persons with device activations when includeDeviceActivations is true`() {
      val personsQueryCriteria = PersonsQueryCriteria(personName = "name", includeDeviceActivations = true)

      val queryExecutionId = "query-execution-id"

      val expectedResult = listOf(
        AthenaPersonDto(
          personId = "personId",
          personName = "name",
          uIdNomis = "nomisId",
          uDob = "1990-01-01",
          zip = "FK12 3FA",
          city = "Fakesville",
          street = "123 Fake Street",
          deviceId = "deviceId",
          deviceActivationId = "deviceActivationId",
          deviceActivationDate = "",
          deviceDeactivationDate = "",
        ),
      )

      whenever(
        queryCacheRepository.findByNomisIdAndPersonNameAndDeviceIdAndIncludeDeviceActivationsAndCreatedAtAfter(
          any(),
          eq("name"),
          any(),
          eq(true),
          any(),
        )
      ).thenReturn(null)

      whenever(personRepository.getPersonsQueryId(personsQueryCriteria)).thenReturn(queryExecutionId)
      whenever(personRepository.getPersonsQueryResults(queryExecutionId)).thenReturn(expectedResult)

      val result = service.getPersons(personsQueryCriteria, "")

      assertThat(result).isInstanceOf(List::class.java)
      assertThat(result.count()).isEqualTo(1)
      assertThat(result.first()).isInstanceOf(PersonDto::class.java)
      assertThat(result.first().deviceActivations).hasSize(1)
    }

    @Test
    fun `it should not save to query cache when existing query found in cache`() {
      val personsQueryCriteria = PersonsQueryCriteria(personName = "name", includeDeviceActivations = false)

      val queryExecutionId = "query-execution-id"

      val expectedResult = listOf(
        AthenaPersonDto(
          personId = "personId",
          personName = "name",
          uIdNomis = "nomisId",
          uDob = "1990-01-01",
          zip = "FK12 3FA",
          city = "Fakesville",
          street = "123 Fake Street",
          deviceId = null,
          deviceActivationId = null,
          deviceActivationDate = null,
          deviceDeactivationDate = null,
        ),
      )

      val personsQuery = PersonsQuery(
        id = UUID.randomUUID(),
        nomisId = null,
        personName = null,
        deviceId = null,
        includeDeviceActivations = false,
        queryExecutionId = queryExecutionId,
        queryOwner = "",
        createdAt = ZonedDateTime.now()
      )

      whenever(
        queryCacheRepository.findByNomisIdAndPersonNameAndDeviceIdAndIncludeDeviceActivationsAndCreatedAtAfter(
          eq(null),
          eq("name"),
          eq(null),
          eq(false),
          any(),
        )
      ).thenReturn(personsQuery)

      whenever(personRepository.getPersonsQueryResults(queryExecutionId)).thenReturn(expectedResult)

      val result = service.getPersons(personsQueryCriteria, "")

      assertThat(result).isInstanceOf(List::class.java)
      assertThat(result.count()).isEqualTo(1)
      assertThat(result.first()).isInstanceOf(PersonDto::class.java)
      verify(queryCacheRepository, times(0))
        .save(any())
      verify(personRepository, times(0))
        .getPersonsQueryId(any())
    }
  }
}