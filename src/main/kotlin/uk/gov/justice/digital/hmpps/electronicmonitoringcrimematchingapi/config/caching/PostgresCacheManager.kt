package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.caching

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person.caching.CacheEntryRepository
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
@EnableConfigurationProperties(
  CacheProperties::class,
)
class PostgresCacheManager(
  private val repository: CacheEntryRepository,
  private val properties: CacheProperties,
) : CacheManager {

  private val caches = ConcurrentHashMap<String, Cache>()
  val defaultTtl: Duration = Duration.ofMinutes(10)

  override fun getCache(name: String): Cache? = caches.computeIfAbsent(name) {
    val cacheConfig = properties.configs[it]
    val ttl = cacheConfig?.ttl ?: defaultTtl
    PostgresCache(it, repository, ttl)
  }

  override fun getCacheNames(): Collection<String> = caches.keys
}
