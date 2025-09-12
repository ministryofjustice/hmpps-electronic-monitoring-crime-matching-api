package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.caching

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.cache")
data class CacheProperties(
  val configs: Map<String, CacheConfig> = emptyMap(),
)
