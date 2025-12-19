package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "crime_batch_email_attachment_ingestion_error")
class CrimeBatchEmailAttachmentIngestionError(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  val rowNumber: Long?,

  val crimeReference: String?,

  val errorType: String,

  @ManyToOne
  @JoinColumn(name = "crime_batch_email_attachment_id")
  val crimeBatchEmailAttachment: CrimeBatchEmailAttachment,
)
