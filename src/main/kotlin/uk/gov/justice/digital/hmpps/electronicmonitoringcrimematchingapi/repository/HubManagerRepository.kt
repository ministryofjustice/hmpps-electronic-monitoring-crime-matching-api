package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.HubManager
import java.util.UUID

@Repository
interface HubManagerRepository : JpaRepository<HubManager, UUID> {
  fun findBySignatureImageIsNotNull(): List<HubManager>
}
