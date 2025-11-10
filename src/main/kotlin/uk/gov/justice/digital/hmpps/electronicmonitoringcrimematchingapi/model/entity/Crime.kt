package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.GeodeticDatum
import java.time.LocalDateTime

@Entity
@Table(name = "crime")
data class Crime(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Enumerated(EnumType.STRING)
  val crimeTypeId: CrimeType,

  val crimeReference: String,

  val crimeDateTimeFrom: LocalDateTime,

  val crimeDateTimeTo: LocalDateTime,

  val easting: Double?,

  val northing: Double?,

  val latitude: Double?,

  val longitude: Double?,

  @Enumerated(EnumType.STRING)
  val datum: GeodeticDatum,

  val crimeText: String,

  @Schema(hidden = true)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "batch_id", nullable = false)
  var crimeBatch: CrimeBatch,
)
