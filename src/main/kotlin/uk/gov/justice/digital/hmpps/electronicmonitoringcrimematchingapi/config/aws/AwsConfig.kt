package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.aws

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.s3.S3AsyncClient
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

    if (properties.athena.endpointUrl != null) {
      clientBuilder.endpointOverride(URI(properties.athena.endpointUrl))
    }

    assumeRoleCredentialsProvider(properties.athena.role)?.let {
      clientBuilder.credentialsProvider(it)
    }

    return clientBuilder.build()
  }

  @Bean
  fun s3Client(): S3Client {
    val clientBuilder = S3Client.builder()
      .region(properties.region)

    if (properties.s3.endpointUrl != null) {
      clientBuilder.endpointOverride(URI(properties.s3.endpointUrl))
      clientBuilder.forcePathStyle(true)
    }

    return clientBuilder.build()
  }

  @Bean
  fun s3AsyncClient(): S3AsyncClient = S3AsyncClient.builder()
    .region(properties.region)
    .apply {
      if (!properties.s3.endpointUrl.isNullOrBlank()) {
        endpointOverride(URI.create(properties.s3.endpointUrl))
        forcePathStyle(true)
      }
      assumeRoleCredentialsProvider(properties.athena.role)?.let { credentialsProvider(it) }
    }
    .build()

  private fun assumeRoleCredentialsProvider(roleArn: String?): StsAssumeRoleCredentialsProvider? = roleArn?.let {
    StsAssumeRoleCredentialsProvider.builder().stsClient(stsClient())
      .refreshRequest { builder -> builder.roleArn(it).roleSessionName(sessionId) }
      .build()
  }
}
