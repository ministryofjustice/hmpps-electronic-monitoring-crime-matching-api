package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.caching

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.caching.CacheEntry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.caching.CacheEntryRepository
import java.time.Duration
import java.time.LocalDateTime

@ActiveProfiles("test")
class PostgresCacheTest {
  private val repository: CacheEntryRepository = mock()
  private val cacheName = "testCache"
  private val ttl = Duration.ofMinutes(1)
  private val cache = PostgresCache(cacheName, repository, ttl)

  private val activeCacheEntry = CacheEntry(
    cacheName = cacheName,
    cacheKey = cache.serialize("active"),
    cacheValue = cache.serialize("value"),
    createdAt = LocalDateTime.now(),
  )

  private val expiredCacheEntry = CacheEntry(
    cacheName = cacheName,
    cacheKey = cache.serialize("expired"),
    cacheValue = cache.serialize("value"),
    createdAt = LocalDateTime.now().minus(ttl).minusSeconds(1),
  )

  @Nested
  @DisplayName("PostgresCache.get()")
  inner class Get {
    @Test
    fun `get should return an active entry`() {
      whenever(
        repository.findByCacheNameAndCacheKey(any(), any()),
      ).thenReturn(activeCacheEntry)

      val retrievedValue = cache.get("active", String::class.java)

      assertThat(retrievedValue).isNotNull()
      assertThat(retrievedValue).isEqualTo("value")
    }

    @Test
    fun `get should return null for an expired entry`() {
      whenever(
        repository.findByCacheNameAndCacheKey(any(), any()),
      ).thenReturn(expiredCacheEntry)

      val retrievedValue = cache.get("expired", String::class.java)

      assertThat(retrievedValue).isNull()
    }

    @Test
    fun `get should return null for a cache miss`() {
      whenever(
        repository.findByCacheNameAndCacheKey(any(), any()),
      ).thenReturn(null)

      val retrievedValue = cache.get("nonExistentKey", String::class.java)

      assertThat(retrievedValue).isNull()
    }
  }

  @Nested
  @DisplayName("PostgresCache.putIfAbsent()")
  inner class PutIfAbsent {
    @Test
    fun `putIfAbsent should create a cache entry if it does not exist`() {
      whenever(
        repository.findByCacheNameAndCacheKey(any(), any()),
      ).thenReturn(null)

      cache.putIfAbsent("active", "value2")

      argumentCaptor<CacheEntry>().apply {
        verify(repository, times(1)).save(capture())
      }
    }

    @Test
    fun `putIfAbsent should not create a cache entry if it already exists`() {
      whenever(
        repository.findByCacheNameAndCacheKey(any(), any()),
      ).thenReturn(activeCacheEntry)

      cache.putIfAbsent("active", "value2")

      argumentCaptor<CacheEntry>().apply {
        verify(repository, times(0)).save(capture())
      }
    }
  }
}
