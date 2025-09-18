package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClientInterface
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.AthenaPersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaHelper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.formatter
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.nullableLocalDateTime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers.PersonMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.DeviceActivation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Person
import java.time.LocalDateTime
import java.util.Optional

@Service
class PersonRepository(
  val athenaClient: EmDatastoreClientInterface,
  val mapper: PersonMapper,
) {

  fun getPersons(personsQueryCriteria: PersonsQueryCriteria): List<Person> {
    val query = GetPersonsQueryBuilder(personsQueryCriteria).build()
    val queryResult = athenaClient.getQueryResult(query)
    val persons = AthenaHelper.Companion.mapTo<AthenaPersonDto>(queryResult)

    if (personsQueryCriteria.includeDeviceActivations) {
      return persons.map { mapper.fromAthenaToModel(it) }
    }

    return mapToModel(persons)
  }

  fun findById(id: Long): Optional<Person> {
    val query = GetPersonByIdQueryBuilder(id).build()
    val queryExecutionId = athenaClient.getQueryExecutionId(query)
    val queryResult = athenaClient.getQueryResult(queryExecutionId)
    val persons = AthenaHelper.Companion.mapTo<AthenaPersonDto>(queryResult)

    return mapToModel(persons.firstOrNull())
  }

  private fun mapToModel(athenaPerson: AthenaPersonDto?): Optional<Person> {
    if (athenaPerson === null) {
      return Optional.empty()
    }

    return Optional.of(mapper.fromAthenaToModel(athenaPerson))
  }

  private fun mapToModel(athenaPersons: List<AthenaPersonDto>): List<Person> = athenaPersons
    .groupBy { it.personId }
    .map { (id, rows) ->
      val first = rows.first()
      val deviceActivations = rows.map {
        DeviceActivation(
          deviceActivationId = it.deviceActivationId!!,
          deviceId = it.deviceId!!,
          deviceName = "",
          personId = it.personId,
          deviceActivationDate = LocalDateTime.parse(it.deviceActivationDate, formatter),
          deviceDeactivationDate = nullableLocalDateTime(it.deviceDeactivationDate),
          orderStart = "",
          orderEnd = "",
        )
      }

      Person(
        personId = first.personId,
        personName = first.personName,
        nomisId = first.uIdNomis,
        pncRef = "",
        dob = first.uDob,
        probationPractitioner = "",
        zip = first.zip,
        city = first.city,
        street = first.street,
        deviceActivations = deviceActivations,
      )
    }
}
