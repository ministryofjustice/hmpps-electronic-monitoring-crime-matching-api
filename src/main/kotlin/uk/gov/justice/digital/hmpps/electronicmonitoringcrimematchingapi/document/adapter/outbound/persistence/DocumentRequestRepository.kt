package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import java.util.UUID

interface DocumentRequestRepository : JpaRepository<DocumentRequestEntity, UUID> {
  fun findFirstByStatus(status: DocumentRequestStatus): DocumentRequestEntity?
}
