package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "crime_matching_result_position")
data class CrimeMatchingResultPosition(
  @Id
  @Column(name = "id", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "crime_matching_result_device_wearer_id", nullable = false)
  val crimeMatchingResultDeviceWearer: CrimeMatchingResultDeviceWearer,

  val latitude: Double,

  val longitude: Double,

  val capturedDateTime: LocalDateTime,

  val sequenceLabel: String,

  val confidenceCircle: Int,
)
