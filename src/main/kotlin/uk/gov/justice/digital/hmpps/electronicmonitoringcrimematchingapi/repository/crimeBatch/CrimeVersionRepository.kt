package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import java.util.UUID

@Repository
interface CrimeVersionRepository : JpaRepository<CrimeVersion, UUID> {
  fun findByCrimeCrimeReferenceContainingIgnoreCase(
    crimeReference: String,
    pageable: Pageable,
  ): Page<CrimeVersion>

  fun findFirstByCrimeIdOrderByCreatedAtDesc(crimeId: UUID): CrimeVersion?
}
