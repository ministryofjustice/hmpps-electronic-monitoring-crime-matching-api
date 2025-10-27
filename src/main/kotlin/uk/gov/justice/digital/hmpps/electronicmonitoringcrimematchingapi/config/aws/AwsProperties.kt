package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.aws

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import software.amazon.awssdk.regions.Region

@ConfigurationProperties(prefix = "aws")
data class AwsProperties(
  @NestedConfigurationProperty
  val athena: AthenaProperties = AthenaProperties(),

  @NestedConfigurationProperty
  val s3: S3Properties = S3Properties(),

  val endpointUrl: String? = null,

  val region: Region,
)
