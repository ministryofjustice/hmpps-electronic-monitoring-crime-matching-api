package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.caching

import org.springframework.boot.context.properties.bind.ConstructorBinding
import java.time.Duration

data class CacheConfig @ConstructorBinding constructor(
  val ttl: Duration,
)
