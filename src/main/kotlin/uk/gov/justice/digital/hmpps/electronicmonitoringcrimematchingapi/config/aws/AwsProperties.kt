package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.aws

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import software.amazon.awssdk.regions.Region

@ConfigurationProperties(prefix = "aws")
data class AwsProperties(
  @NestedConfigurationProperty
  val athena: AthenaProperties = AthenaProperties(),

  val endpointUrl: String? = null,

  val localstackUrl: String? = null,

  val region: Region,
)
