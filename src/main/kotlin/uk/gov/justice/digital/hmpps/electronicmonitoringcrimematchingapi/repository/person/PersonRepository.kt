package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClient
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.AthenaRepository
import java.util.Optional

@Repository
class PersonRepository(
  athenaClient: EmDatastoreClient,
) : AthenaRepository<Person>(athenaClient) {

  override val resultSetExtractor = PersonResultSetExtractor()

  fun getPersons(personsQueryCriteria: PersonsQueryCriteria): List<Person> = this.executeQuery(
    GetPersonsQueryBuilder(
      athenaClient.properties,
      personsQueryCriteria,
    ).build(),
  )

  fun findById(id: Long): Optional<Person> = Optional.ofNullable(
    this.executeQuery(
      GetPersonByIdQueryBuilder(
        athenaClient.properties,
        id,
      ).build(),
    ).firstOrNull(),
  )
}
