package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.BatchIngestionErrorType
import java.util.UUID

@Entity
@Table(name = "crime_batch_ingestion_error")
data class CrimeBatchEmailIngestionError(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val errorType: BatchIngestionErrorType,

  @OneToOne
  @JoinColumn(name = "crime_batch_ingestion_attempt_id")
  val crimeBatchEmail: CrimeBatchEmail,
)
