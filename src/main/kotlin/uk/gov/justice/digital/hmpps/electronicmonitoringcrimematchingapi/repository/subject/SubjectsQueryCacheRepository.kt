package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.subject

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectsQuery
import java.time.ZonedDateTime

@Repository
interface SubjectsQueryCacheRepository : JpaRepository<SubjectsQuery, Long> {
  fun findByNomisIdAndSubjectNameAndCreatedAtAfter(nomisId: String?, name: String?, retentionDate: ZonedDateTime): SubjectsQuery?
}
