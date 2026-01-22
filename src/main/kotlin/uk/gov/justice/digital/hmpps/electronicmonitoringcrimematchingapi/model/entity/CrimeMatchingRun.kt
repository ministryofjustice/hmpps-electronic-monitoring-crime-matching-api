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
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeMatchingStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeMatchingTriggerType
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "crime_matching_run")
data class CrimeMatchingRun(
  @Id
  @Column(name = "id", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "crime_batch_id", nullable = false)
  val crimeBatch: CrimeBatch,

  val algorithmVersion: String,

  @Enumerated(EnumType.STRING)
  val triggerType: CrimeMatchingTriggerType,

  @Enumerated(EnumType.STRING)
  val status: CrimeMatchingStatus,

  val createdAt: LocalDateTime = LocalDateTime.now(),

  val matchingStarted: LocalDateTime,

  val matchingEnded: LocalDateTime,

  @OneToMany(mappedBy = "crimeMatchingRun", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  val results: MutableList<CrimeMatchingResult> = mutableListOf(),
)
