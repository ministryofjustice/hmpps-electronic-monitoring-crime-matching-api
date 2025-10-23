package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "crime")
data class Crime(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  val crimeTypeId: String,
  val crimeTypeDescription: String,
  val crimeReference: String,
  val crimeDateTimeFrom: String,
  val crimeDateTimeTo: String,
  val easting: String?,
  val northing: String?,
  val latitude: String?,
  val longitude: String?,
  val datum: String?,
  val crimeText: String?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "batch_id", nullable = false)
  var crimeBatch: CrimeBatch,
)
