package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PublishMatchingState
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "publish_matching_outbox")
class PublishMatchingOutbox(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @OneToOne
  @JoinColumn(name = "crime_batch_ingestion_attempt_id", nullable = false)
  var crimeBatchIngestionAttempt: CrimeBatchIngestionAttempt,

  @Enumerated(EnumType.STRING)
  var state: PublishMatchingState,

  val createdAt: LocalDateTime = LocalDateTime.now(),
)
