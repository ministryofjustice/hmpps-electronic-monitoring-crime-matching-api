package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.MatchingPublishState
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "crime_batch_ingestion_attempt")
data class CrimeBatchIngestionAttempt(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @OneToOne(mappedBy = "crimeBatchIngestionAttempt", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  var crimeBatchEmail: CrimeBatchEmail? = null,

  val bucket: String,
  val objectName: String,
  val createdAt: LocalDateTime = LocalDateTime.now(),

  /** Denormalised FK to [CrimeBatch] for duplicate-delivery publish retry (null for FAILED/ERROR outcomes). */
  @Column(name = "crime_batch_id")
  var crimeBatchId: UUID? = null,

  /**
   * Whether the matching-notification SNS publish has been confirmed for this attempt.
   * Persisted atomically with the ingestion outcome so duplicate SQS deliveries can
   * decide whether to retry `publishMatchingRequest`.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "matching_publish_state", nullable = false)
  var matchingPublishState: MatchingPublishState = MatchingPublishState.UNKNOWN,
) {
  // JPA entities should not include bidirectional associations in equality.
  override fun equals(other: Any?): Boolean = this === other || (other is CrimeBatchIngestionAttempt && id == other.id)

  override fun hashCode(): Int = id.hashCode()
}
