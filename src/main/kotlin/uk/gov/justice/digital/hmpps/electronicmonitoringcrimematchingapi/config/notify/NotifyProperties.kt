package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.notify

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notify")
data class NotifyProperties(
  val enabled: Boolean,
  val successfulIngestionTemplateId: String,
  val apikey: String,
)
