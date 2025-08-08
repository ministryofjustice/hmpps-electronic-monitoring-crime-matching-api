package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.person

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.athena.repository.PersonsRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.postgres.repository.PersonsQueryCacheRepository
import java.time.ZonedDateTime

@Service
class PersonsService(
  @Autowired val personsQueryCacheRepository: PersonsQueryCacheRepository,
  @Autowired val personsRepository: PersonsRepository,
) {

  @Transactional
  fun getPersonsQueryResults(personsQueryCriteria: PersonsQueryCriteria, user: String): List<Person> {
//    TODO()

    return personsRepository.findByPersonNameLikeIgnoreCase("%${personsQueryCriteria.name}%")

  }

}