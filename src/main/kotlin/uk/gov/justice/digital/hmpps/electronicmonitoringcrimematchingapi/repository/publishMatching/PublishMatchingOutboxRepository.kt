package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.publishMatching

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.PublishMatchingOutbox
import java.util.UUID

@Repository
interface PublishMatchingOutboxRepository : JpaRepository<PublishMatchingOutbox, UUID> {
  @Query(
    value = """
    with candidates as (
      select id
      from publish_matching_outbox
      where state = :pendingState
        and (claimed_at is null or claimed_at < :cutoff)
      order by created_at
      for update skip locked
    )
    update publish_matching_outbox p
    set claimed_at = :now
    from candidates c
    where p.id = c.id
    returning p.*
  """,
    nativeQuery = true,
  )
  fun claimEligibleRows(
    @Param("pendingState") pendingState: String,
    @Param("cutoff") cutoff: Long,
    @Param("now") now: Long,
  ): List<PublishMatchingOutbox>
}
