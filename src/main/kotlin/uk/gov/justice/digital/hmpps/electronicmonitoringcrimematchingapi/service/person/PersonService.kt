package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.person

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.entity.person.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.nullableLocalDateTime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.deviceactivation.DeviceActivationDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person.PersonRepository

@Service
class PersonService(
  @Autowired val personRepository: PersonRepository,
) {

  @Transactional
  fun getPersons(personsQueryCriteria: PersonsQueryCriteria, user: String): List<PersonDto> {
    val res = personRepository.getPersons(personsQueryCriteria)

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

  private fun mapWithDeviceActivations(res: List<Person>): List<PersonDto> = res.groupBy { it.personId }.map { (id, rows) ->
    val first = rows.first()
    val deviceActivations = rows.map {
      DeviceActivationDto(
        deviceActivationId = it.deviceActivationId!!.toInt(),
        deviceId = it.deviceId!!.toInt(),
        deviceName = "",
        personId = it.personId.toInt(),
        deviceActivationDate = nullableLocalDateTime(it.deviceActivationDate),
        deviceDeactivationDate = nullableLocalDateTime(it.deviceDeactivationDate),
        orderStart = "",
        orderEnd = "",
      )
    }

    PersonDto(
      personId = id,
      name = first.personName,
      nomisId = first.uIdNomis,
      pncRef = "",
      dateOfBirth = first.uDob,
      probationPractitioner = "",
      address = "${first.street} ${first.city} ${first.zip}",
      deviceActivations = deviceActivations,
    )
  }

  private fun mapToDto(person: Person): PersonDto = PersonDto(
    personId = person.personId,
    name = person.personName,
    nomisId = person.uIdNomis,
    pncRef = "",
    dateOfBirth = person.uDob,
    probationPractitioner = "",
    address = "${person.street} ${person.city} ${person.zip}",
    deviceActivations = listOf(),
  )
}
