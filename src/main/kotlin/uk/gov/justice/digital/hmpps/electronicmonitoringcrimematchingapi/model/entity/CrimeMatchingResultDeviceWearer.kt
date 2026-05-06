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
  name = "crime_matching_result_device_wearer",
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["crime_matching_result_id", "device_id"]),
  ],
)
data class CrimeMatchingResultDeviceWearer(
  @Id
  @Column(name = "id", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @Column(nullable = false)
  val address: String,

  @Column(nullable = false)
  val dateOfBirth: LocalDateTime,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "crime_matching_result_id", nullable = false)
  val crimeMatchingResult: CrimeMatchingResult,

  val deviceId: Long,

  @Column(nullable = false)
  val identifier: String,

  @Column(nullable = false)
  val name: String,

  @Column(nullable = false)
  val nomisId: String,

  @Column(nullable = false)
  val pncRef: String,

  @OneToMany(mappedBy = "crimeMatchingResultDeviceWearer", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  val positions: MutableList<CrimeMatchingResultPosition> = mutableListOf(),
)
