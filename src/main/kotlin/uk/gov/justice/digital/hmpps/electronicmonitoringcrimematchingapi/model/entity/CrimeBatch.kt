package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "crime_batch")
data class CrimeBatch(
  @Id
  val id: String,
  val policeForce: String,

  @OneToMany(mappedBy = "crimeBatch", cascade = [CascadeType.ALL])
  val crimes: MutableList<Crime> = mutableListOf(),
)
