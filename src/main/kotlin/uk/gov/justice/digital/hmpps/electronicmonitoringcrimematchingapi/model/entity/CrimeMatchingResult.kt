package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
  name = "crime_matching_result",
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["crime_matching_run_id", "crime_version_id"]),
  ],
)
data class CrimeMatchingResult(
  @Id
  @Column(name = "id", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "crime_version_id", nullable = false)
  val crimeVersion: CrimeVersion,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "crime_matching_run_id", nullable = false)
  val crimeMatchingRun: CrimeMatchingRun,

  val createdAt: LocalDateTime = LocalDateTime.now(),

  @OneToMany(mappedBy = "crimeMatchingResult", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  val deviceWearers: MutableList<CrimeMatchingResultDeviceWearer> = mutableListOf(),
)
