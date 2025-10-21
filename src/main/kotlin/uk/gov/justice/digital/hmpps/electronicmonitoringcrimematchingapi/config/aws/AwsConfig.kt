package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.aws

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider
import java.net.URI

@Configuration
@EnableConfigurationProperties(
  AwsProperties::class,
)
class AwsConfig(
  private val properties: AwsProperties,
) {

  val sessionId: String = "CrimeMatchingApiSession"

  @Bean
  fun stsClient(): StsClient {
    val clientBuilder = StsClient.builder()
      .region(properties.region)

    if (properties.endpointUrl != null) {
      clientBuilder.endpointOverride(URI(properties.endpointUrl))
    }

    return clientBuilder.build()
  }

  @Bean
  fun athenaClient(): AthenaClient {
    val clientBuilder = AthenaClient.builder()
      .region(properties.region)

    if (properties.endpointUrl != null) {
      clientBuilder.endpointOverride(URI(properties.endpointUrl))
    }

    if (properties.athena.role != null) {
      clientBuilder.credentialsProvider(
        StsAssumeRoleCredentialsProvider.builder().stsClient(stsClient())
          .refreshRequest { builder ->
            builder.roleArn(properties.athena.role).roleSessionName(sessionId)
          }
          .build(),
      )
    }

    return clientBuilder.build()
  }

  @Bean
  fun s3Client(): S3Client {
    val clientBuilder = S3Client.builder()
      .region(properties.region)

    if (properties.endpointUrl != null) {
      clientBuilder.endpointOverride(URI(properties.endpointUrl))
      clientBuilder.forcePathStyle(true)
    }

    return clientBuilder.build()
  }
}
