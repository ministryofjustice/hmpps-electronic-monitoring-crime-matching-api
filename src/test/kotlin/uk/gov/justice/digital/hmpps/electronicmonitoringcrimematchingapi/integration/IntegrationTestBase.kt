package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.AwsApiExtension
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.AwsApiExtension.Companion.awsMockServer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ExtendWith(HmppsAuthApiExtension::class, AwsApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @BeforeEach
  fun setupBase() {
    awsMockServer.stubStsAssumeRole()
  }

  @AfterEach
  fun teardown() {
    awsMockServer.resetAll()
  }

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  internal fun setAuthorisation(
    username: String? = "AUTH_ADM",
    roles: List<String> = listOf("ROLE_EM_CRIME_MATCHING_GENERAL_RO"),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  protected fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
  }

  protected fun stubQueryExecution(
    queryExecutionId: String,
    queryExecutionStatus: String,
    queryResponseFile: String,
  ) {
    awsMockServer.stubAthenaStartQueryExecution(queryExecutionId)
    awsMockServer.stubAthenaGetQueryExecution(queryExecutionStatus)
    awsMockServer.stubAthenaGetQueryResults(queryResponseFile)
  }
}
