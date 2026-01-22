package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingRun
import java.util.UUID

@Repository
interface CrimeMatchingRunRepository : JpaRepository<CrimeMatchingRun, UUID>
