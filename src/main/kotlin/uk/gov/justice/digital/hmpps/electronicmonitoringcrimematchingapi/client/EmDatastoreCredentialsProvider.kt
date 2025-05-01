package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider

class EmDatastoreCredentialsProvider {
  companion object {
    const val SESSION_ID: String = "DataStoreApiSession"
    val region: Region = Region.EU_WEST_2

    fun getCredentials(iamRole: String): AwsCredentialsProvider {
      val useLocalCredentials: Boolean = System.getenv("FLAG_USE_LOCAL_CREDS").toBoolean()

      return if (useLocalCredentials) {
        EnvironmentVariableCredentialsProvider.create()
      } else {
        getModernisationPlatformCredentialsProvider(iamRole)
      }
    }

    fun getModernisationPlatformCredentialsProvider(iamRole: String): StsAssumeRoleCredentialsProvider {
      val stsClient = StsClient.builder()
        .credentialsProvider(DefaultCredentialsProvider.builder().build())
        .region(region)
        .build()

      val credentialsProvider = StsAssumeRoleCredentialsProvider.builder()
        .stsClient(stsClient)
        .refreshRequest { builder ->
          builder.roleArn(iamRole).roleSessionName(SESSION_ID)
        }
        .build()

      return credentialsProvider
    }
  }
}
