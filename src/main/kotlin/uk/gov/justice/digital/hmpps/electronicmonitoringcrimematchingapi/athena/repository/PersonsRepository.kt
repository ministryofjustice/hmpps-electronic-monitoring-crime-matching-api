package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.athena.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.Person

@Repository
interface PersonsRepository : JpaRepository<Person, Long> {
  fun findByPersonNameLikeIgnoreCase(name: String): List<Person>
}