package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.service.notify.NotificationClient

@Configuration
@EnableConfigurationProperties(
  NotifyProperties::class,
)
class NotifyConfig(@Value("\${notify.apikey}") private val apiKey: String) {

  @Bean
  fun notifyClient(): NotificationClient = NotificationClient(apiKey)
}
