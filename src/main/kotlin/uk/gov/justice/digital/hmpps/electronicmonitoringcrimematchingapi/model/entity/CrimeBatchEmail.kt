package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "crime_batch_email")
data class CrimeBatchEmail(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @Schema(hidden = true)
  @OneToOne
  @JoinColumn(name = "crime_batch_ingestion_attempt_id")
  var crimeBatchIngestionAttempt: CrimeBatchIngestionAttempt,

  val sender: String,
  val originalSender: String,
  val subject: String,
  val sentAt: LocalDateTime,
)