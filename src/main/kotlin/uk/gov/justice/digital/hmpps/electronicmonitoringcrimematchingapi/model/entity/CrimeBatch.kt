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
@Table(name = "crime_batch")
data class CrimeBatch(
  @Id
  @Column(name = "ID", nullable = false, unique = true)
  val id: UUID = UUID.randomUUID(),

  @Enumerated(EnumType.STRING)
  val policeForce: PoliceForce,

  @OneToMany(mappedBy = "crimeBatch", cascade = [CascadeType.ALL], orphanRemoval = true)
  val crimes: MutableList<Crime> = mutableListOf(),
)
