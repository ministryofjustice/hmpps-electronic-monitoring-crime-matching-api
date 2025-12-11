package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
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

  @OneToMany(mappedBy = "crimeBatchEmail", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
  var crimeBatchEmailAttachments: MutableList<CrimeBatchEmailAttachment> = mutableListOf(),

  val sender: String,
  val originalSender: String,
  val subject: String,
  val sentAt: LocalDateTime,
)