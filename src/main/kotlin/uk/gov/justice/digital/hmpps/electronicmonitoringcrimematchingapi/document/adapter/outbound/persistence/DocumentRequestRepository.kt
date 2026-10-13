package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import java.time.Instant
import java.util.*

@Repository
interface DocumentRequestRepository : JpaRepository<DocumentRequestEntity, UUID> {
  @Query(
    value = """
    with candidates as (
      select id
      from document_request
      where (status = DocumentRequestStatus.PENDING)
        and (type = :documentType)
        and (claimed_at is null or claimed_at < :cutoff)
      order by created_at
      limit :batchSize
      for update skip locked
    )
    update document_request dr
    set claimed_at = :now
    from candidates c
    where dr.id = c.id
    returning dr.*
  """,
    nativeQuery = true,
  )
  fun claimEligibleDocumentRequests(
    @Param("documentType") documentType: DocumentType,
    @Param("batchSize") batchSize: Int,
    @Param("cutoff") cutoff: Instant,
    @Param("now") now: Instant,
  ): Collection<DocumentRequestEntity>

  // TODO causes compile error: No property 'updateStatus' found for type 'DocumentRequestEntity'
  // I want to update the status of a specific document request and throw exception if it doesn't exist

  // TODO version field
  @Modifying
  @Query(
    """update DocumentRequestEntity dr    set dr.status = :status    where dr.id = :id"""
  )
  fun updateStatus(
    @Param("id") id: UUID,
    @Param("status") status: DocumentRequestStatus,
  ) // : Int
}
