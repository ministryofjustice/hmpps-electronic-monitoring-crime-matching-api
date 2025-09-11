package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.caching

import org.springframework.cache.Cache
import org.springframework.cache.support.SimpleValueWrapper
import org.springframework.dao.DataRetrievalFailureException
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.caching.CacheEntry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person.caching.CacheEntryRepository
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.Callable

class PostgresCache(
  private val name: String,
  private val repository: CacheEntryRepository,
  val ttl: Duration,
) : Cache {

  override fun getName(): String = name

  override fun getNativeCache(): Any = repository

  override fun get(key: Any): Cache.ValueWrapper? {
    val keyBytes = serialize(key)
    val entry = repository.findByCacheNameAndCacheKey(name, keyBytes)

    return entry?.let {
      if (it.createdAt.plus(ttl).isBefore(LocalDateTime.now())) {
        return null
      }

      val value = deserialize(it.cacheValue)
      SimpleValueWrapper(value)
    }
  }

  override fun <T> get(key: Any, type: Class<T>?): T? {
    val wrapper = get(key)
    if (wrapper?.get() == null) {
      return null
    }
    return type?.cast(wrapper.get())
  }

  override fun <T> get(key: Any, valueLoader: Callable<T>): T? {
    val value = get(key)?.get()
    if (value != null) {
      return value as T
    }

    try {
      val result = valueLoader.call()
      put(key, result)
      return result
    } catch (ex: Exception) {
      throw DataRetrievalFailureException("Failed to load value from valueLoader", ex)
    }
  }

  override fun put(key: Any, value: Any?) {
    value?.let {
      val keyBytes = serialize(key)
      val valueBytes = serialize(it)
      val entry = CacheEntry(
        cacheName = name,
        cacheKey = keyBytes,
        cacheValue = valueBytes,
      )
      repository.save(entry)
    }
  }

  override fun putIfAbsent(key: Any, value: Any?): Cache.ValueWrapper? {
    val existing = get(key)
    if (existing == null) {
      put(key, value)
    }
    return existing
  }

  override fun evict(key: Any) {
    val keyBytes = serialize(key)
    repository.deleteByCacheNameAndCacheKey(name, keyBytes)
  }

  override fun clear() {
    repository.deleteAllByCacheName(name)
  }

  // Helper methods to serialize/deserialize objects to/from byte arrays
  fun serialize(obj: Any): ByteArray {
    val bos = ByteArrayOutputStream()
    val oos = ObjectOutputStream(bos)
    oos.writeObject(obj)
    oos.flush()
    return bos.toByteArray()
  }

  fun deserialize(bytes: ByteArray): Any {
    val bis = ByteArrayInputStream(bytes)
    val ois = ObjectInputStream(bis)
    return ois.readObject()
  }
}
