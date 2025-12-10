package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import java.util.UUID

@Entity
@Table(name = "crime")
data class Crime(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @Enumerated(EnumType.STRING)
  val policeForceArea: PoliceForce,
  val crimeReference: String,

  @OneToMany(mappedBy = "crime", cascade = [CascadeType.ALL], orphanRemoval = true)
  val crimeVersions: MutableList<CrimeVersion> = mutableListOf(),

)
