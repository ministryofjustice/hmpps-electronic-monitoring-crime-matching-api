package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.datastore

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "datastore")
data class DatastoreProperties(
  val outputBucketArn: String,
  val retryIntervalMs: Long = 1000,
)
