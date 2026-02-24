package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration

import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.PostgreSQLContainer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures.CrimeMatchingFixtures
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures.TestFixturesConfig
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.AwsApiExtension
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.AwsApiExtension.Companion.awsMockServer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.caching.CacheEntryRepository
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ExtendWith(HmppsAuthApiExtension::class, AwsApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
@Import(TestFixturesConfig::class)
abstract class IntegrationTestBase {

  companion object {
    @JvmStatic
    private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:18")
      .apply {
        withUsername("postgres")
        withPassword("postgres")
        withDatabaseName("testdb")
        withReuse(true)
      }

    @BeforeAll
    @JvmStatic
    fun startContainers() {
      postgresContainer.start()
    }

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      registry.add("spring.datasource.url") { postgresContainer.jdbcUrl }
      registry.add("spring.datasource.username") { postgresContainer.username }
      registry.add("spring.datasource.password") { postgresContainer.password }
      registry.add("spring.flyway.url") { postgresContainer.jdbcUrl }
      registry.add("spring.flyway.user") { postgresContainer.username }
      registry.add("spring.flyway.password") { postgresContainer.password }
    }

  }

  @Autowired
  lateinit var cacheEntryRepository: CacheEntryRepository

  @Autowired
  lateinit var crimeMatchingFixtures: CrimeMatchingFixtures

  @BeforeEach
  fun setupBase() {
    awsMockServer.stubStsAssumeRole()
    cacheEntryRepository.deleteAll()
    crimeMatchingFixtures.deleteAll()
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
    retryCount: Int,
    finalQueryExecutionStatus: String,
    queryResponseFile: String,
  ) {
    awsMockServer.stubAthenaStartQueryExecution(queryExecutionId)
    awsMockServer.stubAthenaGetQueryExecution(retryCount, finalQueryExecutionStatus)
    awsMockServer.stubAthenaGetQueryResults(queryResponseFile)
  }

  protected fun stubFailedQueryExecution(
    queryExecutionId: String,
  ) {
    awsMockServer.stubAthenaStartQueryExecution(queryExecutionId)
    awsMockServer.stubAthenaGetQueryExecution(1, "FAILED")
  }

  protected fun verifyAthenaStartQueryExecutionCount(
    count: Int,
  ) {
    awsMockServer.verify(
      count,
      postRequestedFor(urlPathEqualTo("/"))
        .withHeader("X-Amz-Target", equalTo("AmazonAthena.StartQueryExecution")),
    )
  }

  protected fun verifyAthenaGetQueryExecutionCount(
    count: Int,
  ) {
    awsMockServer.verify(
      count,
      postRequestedFor(urlPathEqualTo("/"))
        .withHeader("X-Amz-Target", equalTo("AmazonAthena.GetQueryExecution")),
    )
  }

  protected fun verifyAthenaGetQueryResultsCount(
    count: Int,
  ) {
    awsMockServer.verify(
      count,
      postRequestedFor(urlPathEqualTo("/"))
        .withHeader("X-Amz-Target", equalTo("AmazonAthena.GetQueryResults")),
    )
  }

  protected fun verifyAthenaStartQueryExecutionWithQuery(
    query: String,
    executionParameters: List<String>,
  ) {
    val requestPattern = postRequestedFor(urlPathEqualTo("/"))
      .withHeader("X-Amz-Target", equalTo("AmazonAthena.StartQueryExecution"))
      .withRequestBody(
        matchingJsonPath("QueryString", containing(query)),
      )

    for (executionParameter in executionParameters) {
      requestPattern.withRequestBody(
        matchingJsonPath("$.ExecutionParameters[?(@ == \"$executionParameter\")]"),
      )
    }

    awsMockServer.verify(
      1,
      requestPattern,
    )
  }

  protected fun String.loadJson(): String = IntegrationTestBase::class.java.getResource("resource/$this.json")!!.readText()
}
