package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "crime_batch_ingestion_error")
data class CrimeBatchIngestionError(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  val errorType: String,

  @OneToOne
  @JoinColumn(name = "crime_batch_ingestion_attempt_id")
  val crimeBatchIngestionAttempt: CrimeBatchIngestionAttempt,
)
