package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.postgres.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.postgres.entity.PersonsQuery
import java.time.ZonedDateTime

@Repository
interface PersonsQueryCacheRepository : JpaRepository<PersonsQuery, Long> {
  fun findByNomisIdAndPersonNameAndCreatedAtAfter(nomisId: String?, personName: String?, retentionDate: ZonedDateTime): PersonsQuery?
}