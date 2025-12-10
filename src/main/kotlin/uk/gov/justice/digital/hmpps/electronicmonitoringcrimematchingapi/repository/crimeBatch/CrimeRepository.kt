package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import java.util.Optional

@Repository
interface CrimeRepository : JpaRepository<Crime, Long> {
  fun findByCrimeReferenceAndPoliceForceArea(crimeReference: String, policeForce: PoliceForce): Optional<Crime>
}
