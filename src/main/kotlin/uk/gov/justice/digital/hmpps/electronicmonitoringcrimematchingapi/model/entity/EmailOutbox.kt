package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.EmailOutboxState
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "email_outbox")
class EmailOutbox(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @Column(nullable = false, columnDefinition = "TEXT")
  val payload: String,

  @Enumerated(EnumType.STRING)
  var state: EmailOutboxState,

  var attempts: Int = 0,

  @Column(nullable = true, columnDefinition = "TEXT")
  var lastError: String? = null,

  @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
  var claimedAt: Instant? = null,

  @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
  val createdAt: Instant = Instant.now(),

  @Version
  @Column(nullable = false)
  var version: Long = 0,
)
