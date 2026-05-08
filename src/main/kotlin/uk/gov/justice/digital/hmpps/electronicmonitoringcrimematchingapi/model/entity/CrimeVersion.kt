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

  @OneToMany(mappedBy = "crimeVersion", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  val matchingResults: MutableList<CrimeMatchingResult> = mutableListOf(),

  @Column(nullable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
) {
  val versionLabel: String
    get() {
      val versions = crime.crimeVersions.sortedBy { it.createdAt }
      val currentVersionIndex = crime.crimeVersions.indexOf(this)

      val versionNumber = versions
        .take(currentVersionIndex + 1)
        .count { it.updates.isNotEmpty() } + 1

      val previousVersionNumber = if (currentVersionIndex > 0) {
        versions
          .take(currentVersionIndex)
          .count { it.updates.isNotEmpty() } + 1
      } else {
        null
      }

      val isDuplicate = previousVersionNumber != null && versionNumber == previousVersionNumber

      return buildString {
        append(if (isLatest) "Latest version" else "Version $versionNumber")
        if (isDuplicate) append(" (Duplicate)")
      }
    }

  val isLatest: Boolean
    get() {
      return crime.latestVersion.id == this.id
    }

  val updatesSummary: String
    get() {
      val isFirstVersion = crime.crimeVersions
        .minByOrNull { it.createdAt } == this

      if (isFirstVersion) return "N/A"

      if (updates.isEmpty()) return "None"

      val grouped = mutableSetOf<String>()
      val individual = mutableListOf<String>()

      updates.map { it.fieldName }.toSet().forEach {
        val group = it.groupLabel()
        if (group != null) {
          grouped.add(group)
        } else {
          individual.add(it.value)
        }
      }

      return (grouped + individual).joinToString(", ")
    }
}
