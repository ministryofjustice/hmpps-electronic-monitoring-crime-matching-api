package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.person

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person.PersonRepository

@Service
class PersonService(
  val personRepository: PersonRepository,
) {
  fun getPersons(personsQueryCriteria: PersonsQueryCriteria): List<Person> = this.personRepository
    .getPersons(personsQueryCriteria)

  fun getPerson(id: Long): Person = this.personRepository
    .findById(id)
    .orElseThrow {
      EntityNotFoundException("No person found with id: $id")
    }
}
