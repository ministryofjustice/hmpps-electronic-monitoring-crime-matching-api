package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.data.ValidationErrors.CrimeBatch.INVALID_POLICE_FORCE
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.validation.annotation.ValidEnum

@Entity
@Table(name = "crime_batch")
data class CrimeBatch(
  @Id
  val id: String,
  @field:ValidEnum(enumClass = PoliceForce::class, message = INVALID_POLICE_FORCE)
  val policeForce: String?,

  @OneToMany(mappedBy = "crimeBatch", cascade = [CascadeType.ALL], orphanRemoval = true)
  val crimes: MutableList<Crime> = mutableListOf(),
)
