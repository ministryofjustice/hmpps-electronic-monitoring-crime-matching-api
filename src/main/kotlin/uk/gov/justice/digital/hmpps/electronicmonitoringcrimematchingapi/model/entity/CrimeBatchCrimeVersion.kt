package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "crime_batch_crime_version")
data class CrimeBatchCrimeVersion(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  val crimeVersionId: UUID,

  val batchId: String,

//  @OneToOne
//  @JoinColumn(name = "crime_version_id")
//  val crimeVersion: CrimeVersion,
//
//  @ManyToMany
//  @JoinColumn(name = "batch_id")
//  val crimeBatch: CrimeBatch
)