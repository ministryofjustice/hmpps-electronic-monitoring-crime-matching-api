package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.service.notify.NotificationClient

@Configuration
@EnableConfigurationProperties(
  NotifyProperties::class,
)
class NotifyConfig(private val properties: NotifyProperties) {

  @Bean
  fun notifyClient(): NotificationClient = NotificationClient(properties.apikey)
}
