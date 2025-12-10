package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "crime_batch_ingestion_attempt")
data class CrimeBatchIngestionAttempt(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  val bucket: String,
  val objectName: String,
  val createdAt: LocalDateTime = LocalDateTime.now(),
)
