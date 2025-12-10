package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchCrimeVersion

@Repository
interface CrimeBatchCrimeVersionRepository : JpaRepository<CrimeBatchCrimeVersion, Long> {
  fun findCrimeBatchCrimeVersionsByBatchId(batchId: String): List<CrimeBatchCrimeVersion>
}
