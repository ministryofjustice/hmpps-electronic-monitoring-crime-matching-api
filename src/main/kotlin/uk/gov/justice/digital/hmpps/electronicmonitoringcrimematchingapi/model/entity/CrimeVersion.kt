package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "crime_version")
class CrimeVersion(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "crime_id")
  var crime: Crime,

  @Enumerated(EnumType.STRING)
  val crimeTypeId: CrimeType,

  val crimeDateTimeFrom: LocalDateTime,

  val crimeDateTimeTo: LocalDateTime,

  val easting: Double?,

  val northing: Double?,

  val latitude: Double?,

  val longitude: Double?,

  val crimeText: String,

  val createdAt: LocalDateTime = LocalDateTime.now(),
)
