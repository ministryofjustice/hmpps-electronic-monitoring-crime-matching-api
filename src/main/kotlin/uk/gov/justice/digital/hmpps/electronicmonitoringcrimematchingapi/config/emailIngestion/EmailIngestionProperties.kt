package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.emailIngestion

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "email-ingestion")
data class EmailIngestionProperties(
  val mailboxAddress: String,
  val validEmails: Map<String, String> = emptyMap(),
)
