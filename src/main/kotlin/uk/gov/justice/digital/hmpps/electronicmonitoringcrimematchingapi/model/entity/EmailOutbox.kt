package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.EmailOutboxStatus
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "email_outbox")
class EmailOutbox(
  @Id
  @Column(name = "event_id", nullable = false, unique = true)
  val eventId: UUID = UUID.randomUUID(),

  @Column(name = "crime_batch_id")
  val crimeBatchId: UUID? = null,

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  var status: EmailOutboxStatus = EmailOutboxStatus.PENDING,

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
  val payload: String,

  @Column(name = "attempts", nullable = false)
  var attempts: Int = 0,

  @Column(name = "available_at", nullable = false)
  var availableAt: LocalDateTime = LocalDateTime.now(),

  @Column(name = "claimed_at")
  var claimedAt: LocalDateTime? = null,

  @Column(name = "claimed_by")
  var claimedBy: String? = null,

  @Column(name = "last_error")
  var lastError: String? = null,

  @Column(name = "created_at", nullable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),

  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
)
