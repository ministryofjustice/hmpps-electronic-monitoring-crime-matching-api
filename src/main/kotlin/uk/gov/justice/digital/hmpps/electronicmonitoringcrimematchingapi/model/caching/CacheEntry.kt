package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.caching

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "application_cache")
data class CacheEntry(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  val cacheName: String,

  @Column(columnDefinition = "bytea")
  val cacheKey: ByteArray,

  @Column(columnDefinition = "bytea")
  val cacheValue: ByteArray,

  val createdAt: LocalDateTime = LocalDateTime.now(),
)
