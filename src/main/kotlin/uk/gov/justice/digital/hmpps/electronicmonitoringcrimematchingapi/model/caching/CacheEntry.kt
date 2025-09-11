package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.caching

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "application_cache")
data class CacheEntry(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  val cacheName: String,

  @Lob
  val cacheKey: ByteArray,

  @Lob
  val cacheValue: ByteArray,

  val createdAt: LocalDateTime = LocalDateTime.now(),
)
