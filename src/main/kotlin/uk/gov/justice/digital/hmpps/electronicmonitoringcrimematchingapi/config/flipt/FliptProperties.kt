package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.flipt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "flipt")
data class FliptProperties(
  val url: String,
  val namespace: String,
  val pollingIntervalSeconds: Long = 120,
)
