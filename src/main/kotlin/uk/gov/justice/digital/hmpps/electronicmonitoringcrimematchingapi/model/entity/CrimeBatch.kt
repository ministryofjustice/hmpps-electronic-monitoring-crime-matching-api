package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
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

  @ManyToMany(cascade = [CascadeType.ALL])
  @JoinTable(
    name = "crime_batch_crime_version",
    joinColumns = [JoinColumn(name = "crime_batch_id")],
    inverseJoinColumns = [JoinColumn(name = "crime_version_id")],
    uniqueConstraints = [
      UniqueConstraint(columnNames = ["crime_batch_id", "crime_version_id"]),
    ],
  )
  val crimeVersions: MutableSet<CrimeVersion> = mutableSetOf(),

  val createdAt: LocalDateTime = LocalDateTime.now(),

)
