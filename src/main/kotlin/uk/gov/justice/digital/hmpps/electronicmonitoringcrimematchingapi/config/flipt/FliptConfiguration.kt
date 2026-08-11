package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.flipt

import io.flipt.client.FliptClient
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableConfigurationProperties(FliptProperties::class)
class FliptConfiguration(
  private val properties: FliptProperties,
) {
  @Bean
  fun fliptClient() = FliptClient
    .builder()
    .namespace(properties.namespace)
    .url(properties.url)
    .updateInterval(Duration.ofSeconds(properties.pollingIntervalSeconds))
    .build()
}
