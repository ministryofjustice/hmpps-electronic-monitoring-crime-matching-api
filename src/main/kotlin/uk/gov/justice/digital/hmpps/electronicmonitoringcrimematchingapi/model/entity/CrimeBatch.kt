package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

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
@Table(name = "crime_batch")
data class CrimeBatch(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @Column(nullable = false)
  val batchId: String,

  @OneToOne
  @JoinColumn(name = "crime_batch_email_attachment_id", nullable = false)
  val crimeBatchEmailAttachment: CrimeBatchEmailAttachment,

  @OneToMany(mappedBy = "crimeBatch", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  val crimeVersions: MutableList<CrimeVersion> = mutableListOf(),

  val createdAt: LocalDateTime = LocalDateTime.now(),

)
