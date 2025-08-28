package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.entity.person.PersonsQuery
import java.time.ZonedDateTime

@Repository
interface PersonsQueryCacheRepository : JpaRepository<PersonsQuery, Long> {
  fun findByNomisIdAndPersonNameAndDeviceIdAndIncludeDeviceActivationsAndCreatedAtAfter(
    nomisId: String?, name: String?, deviceId: String?, includeDeviceActivations: Boolean, retentionDate: ZonedDateTime
  ): PersonsQuery?
}
