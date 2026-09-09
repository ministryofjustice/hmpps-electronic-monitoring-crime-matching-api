package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
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

  @Column(nullable = false, columnDefinition = "TEXT")
  val payload: String,

  @Enumerated(EnumType.STRING)
  var state: PublishMatchingState,

  var attempts: Int = 0,

  @Column(nullable = true, columnDefinition = "TEXT")
  var lastError: String? = null,

  var claimedAt: LocalDateTime? = null,

  val createdAt: LocalDateTime = LocalDateTime.now(),
)
