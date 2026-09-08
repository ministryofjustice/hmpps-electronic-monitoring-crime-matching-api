package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.publishMatching

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.PublishMatchingOutbox
import java.util.UUID

@Repository
interface PublishMatchingOutboxRepository : JpaRepository<PublishMatchingOutbox, UUID> {
  fun findByCrimeBatchIngestionAttempt(ingestionAttempt: CrimeBatchIngestionAttempt): PublishMatchingOutbox?
}
