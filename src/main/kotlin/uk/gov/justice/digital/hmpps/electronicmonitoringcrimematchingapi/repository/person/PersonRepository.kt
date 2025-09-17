package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClientInterface
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.entity.person.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaHelper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonsQueryCriteria

@Service
class PersonRepository(
  val athenaClient: EmDatastoreClientInterface,
) {

  fun getPersons(personsQueryCriteria: PersonsQueryCriteria): List<Person> {
    val query = GetPersonsQueryBuilder(personsQueryCriteria).build()
    val queryExecutionId = athenaClient.getQueryExecutionId(query)
    val queryResult = athenaClient.getQueryResult(queryExecutionId)
    return AthenaHelper.Companion.mapTo<Person>(queryResult)
  }

  fun getPersonById(id: Long): Person {
    val query = GetPersonByIdQueryBuilder(id).build()
    val queryExecutionId = athenaClient.getQueryExecutionId(query)
    val queryResult = athenaClient.getQueryResult(queryExecutionId)
    val persons = AthenaHelper.Companion.mapTo<Person>(queryResult)

    if (persons.isEmpty()) {
      throw EntityNotFoundException("No person found with id: $id")
    }

    return persons.first()
  }
}
