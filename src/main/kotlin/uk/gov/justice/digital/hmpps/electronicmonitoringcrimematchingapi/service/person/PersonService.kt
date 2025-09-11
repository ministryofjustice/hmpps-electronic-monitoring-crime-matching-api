package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.person

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.entity.person.PersonsQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.nullableLocalDateTime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaPersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.deviceactivation.DeviceActivationDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person.PersonRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person.PersonsQueryCacheRepository
import java.time.ZonedDateTime
import kotlin.collections.component1
import kotlin.collections.component2

@Service
class PersonService(
  @Autowired val personsQueryCacheRepository: PersonsQueryCacheRepository,
  @Autowired val personRepository: PersonRepository,
) {

  @Transactional
  fun getPersons(personsQueryCriteria: PersonsQueryCriteria, user: String): List<PersonDto> {
    val queryExecutionId = getQueryExecutionId(personsQueryCriteria, user)
    val res = personRepository.getPersonsQueryResults(queryExecutionId)

    return if (personsQueryCriteria.includeDeviceActivations) {
      mapWithDeviceActivations(res)
    } else {
      return res.map { PersonDto(it) }
    }
  }

  fun getPerson(id: Long, user: String): PersonDto {
    val person = this.personRepository.getPersonById(id)
    return mapToDto(person)
  }

  private fun getQueryExecutionId(personsQueryCriteria: PersonsQueryCriteria, user: String): String {
    var queryExecutionId = personsQueryCacheRepository.findByNomisIdAndPersonNameAndDeviceIdAndIncludeDeviceActivationsAndCreatedAtAfter(
      personsQueryCriteria.nomisId,
      personsQueryCriteria.personName,
      personsQueryCriteria.deviceId,
      personsQueryCriteria.includeDeviceActivations,
      ZonedDateTime.now().minusDays(1),
    )?.queryExecutionId

    if (queryExecutionId == null) {
      queryExecutionId = personRepository.getPersonsQueryId(personsQueryCriteria)

      personsQueryCacheRepository.save(
        PersonsQuery(
          nomisId = personsQueryCriteria.nomisId,
          personName = personsQueryCriteria.personName,
          deviceId = personsQueryCriteria.deviceId,
          includeDeviceActivations = personsQueryCriteria.includeDeviceActivations,
          queryExecutionId = queryExecutionId,
          queryOwner = user,
        ),
      )
    }
    return queryExecutionId
  }

  private fun mapWithDeviceActivations(res: List<AthenaPersonDto>): List<PersonDto> = res.groupBy { it.personId }.map { (id, rows) ->
    val first = rows.first()
    val deviceActivations = rows.map {
      DeviceActivationDto(
        deviceId = it.deviceId!!,
        deviceActivationId = it.deviceActivationId!!,
        deviceActivationDate = nullableLocalDateTime(it.deviceActivationDate),
        deviceDeactivationDate = nullableLocalDateTime(it.deviceDeactivationDate),
      )
    }

    PersonDto(
      personId = id,
      personName = first.personName,
      nomisId = first.uIdNomis,
      pncRef = "",
      dateOfBirth = first.uDob,
      address = "${first.street} ${first.city} ${first.zip}",
      deviceActivations = deviceActivations,
    )
  }

  private fun mapToDto(athenaPerson: AthenaPersonDto): PersonDto = PersonDto(
    personId = athenaPerson.personId,
    personName = athenaPerson.personName,
    nomisId = athenaPerson.uIdNomis,
    pncRef = "",
    dateOfBirth = athenaPerson.uDob,
    address = "${athenaPerson.street} ${athenaPerson.city} ${athenaPerson.zip}",
    deviceActivations = listOf(),
  )
}
