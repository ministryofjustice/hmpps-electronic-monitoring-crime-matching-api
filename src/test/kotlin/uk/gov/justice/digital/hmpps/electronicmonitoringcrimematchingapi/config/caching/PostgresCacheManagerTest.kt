package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.caching

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person.caching.CacheEntryRepository
import java.time.Duration

@ActiveProfiles("test")
class PostgresCacheManagerTest {
  private val repository: CacheEntryRepository = mock()
  private val cacheProperties: CacheProperties = mock()

  @BeforeEach
  fun setUp() {
    whenever(cacheProperties.configs).thenReturn(
      mapOf("testCache" to CacheConfig(Duration.ofMinutes(5))),
    )
  }

  @Test
  fun `getCache should return a cache configured by application properties`() {
    val cacheManager = PostgresCacheManager(repository, cacheProperties)
    val cache = cacheManager.getCache("testCache") as PostgresCache?

    assertThat(cache).isNotNull()
    assertThat(cache?.name).isEqualTo("testCache")
    assertThat(cache?.ttl).isEqualTo(Duration.ofMinutes(5))
  }

  @Test
  fun `getCache should return a cache with default Ttl`() {
    val cacheManager = PostgresCacheManager(repository, cacheProperties)
    val cache = cacheManager.getCache("otherCache") as PostgresCache?

    assertThat(cache).isNotNull()
    assertThat(cache?.name).isEqualTo("otherCache")
    assertThat(cache?.ttl).isEqualTo(cacheManager.defaultTtl)
  }
}
