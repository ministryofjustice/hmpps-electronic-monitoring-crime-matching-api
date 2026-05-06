package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "crime_version")
data class CrimeVersion(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "crime_id", nullable = false)
  var crime: Crime,

  @ManyToOne
  @JoinColumn(name = "crime_batch_id")
  var crimeBatch: CrimeBatch,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val crimeTypeId: CrimeType,

  @Column(nullable = false)
  val crimeDateTimeFrom: Instant,

  @Column(nullable = false)
  val crimeDateTimeTo: Instant,

  val easting: Double?,

  val northing: Double?,

  val latitude: Double?,

  val longitude: Double?,

  @Column(nullable = false)
  val crimeText: String,

  @OneToMany(mappedBy = "crimeVersion", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  val updates: MutableList<CrimeVersionUpdate> = mutableListOf(),

  @Column(nullable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
)
