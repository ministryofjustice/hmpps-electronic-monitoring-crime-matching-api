package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import java.util.UUID

@Entity
@Table(name = "crime",
  uniqueConstraints = [
    UniqueConstraint(columnNames = ["police_force_area", "crime_reference"]),
  ],
)
data class Crime(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @Enumerated(EnumType.STRING)
  val policeForceArea: PoliceForce,
  val crimeReference: String,

  @Schema(hidden = true)
  @OneToMany(mappedBy = "crime", fetch = FetchType.LAZY, orphanRemoval = true)
  val crimeVersions: MutableList<CrimeVersion> = mutableListOf(),

)
