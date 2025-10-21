package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "crime_batch")
data class CrimeBatch(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  val policeForce: String,
  val crimeTypeId: String,
  val crimeTypeDescription: String,
  val batchId: String,
  val crimeId: String,
  val crimeDateTimeFrom: String,
  val crimeDateTimeTo: String,
  val easting: String?,
  val northing: String?,
  val latitude: String?,
  val longitude: String?,
  val datum: String?,
  val crimeText: String?,
)