package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
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

  val batchId : String,

  @OneToOne
  @JoinColumn(name = "crime_batch_email_attachment_id")
  val crimeBatchEmailAttachment: CrimeBatchEmailAttachment,

  @Schema(hidden = true)
  @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE])
  @JoinTable(
    name = "crime_batch_crime_version",
    joinColumns = [JoinColumn(name = "crime_batch_id")],
    inverseJoinColumns = [JoinColumn(name = "crime_version_id")]
  )
  val crimeVersions: MutableList<CrimeVersion> = mutableListOf(),

  val createdAt: LocalDateTime = LocalDateTime.now(),

)
