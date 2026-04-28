package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.HubManager
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.HubManagerRepository
import java.nio.charset.StandardCharsets
import java.util.UUID

@ActiveProfiles("integration")
class HubManagerControllerTest : IntegrationTestBase() {
  val path = "/hub-managers"

  @Autowired
  lateinit var repo: HubManagerRepository

  @BeforeEach
  fun setup() {
    repo.deleteAll()
  }

  @Nested
  @DisplayName("GET /hub-managers")
  inner class GetHubManagers {
    @Test
    fun `it should return 401 if the request is not authenticated`() {
      webTestClient.get()
        .uri(path)
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return a 403 if the client does not have a valid role`() {
      webTestClient.get()
        .uri(path)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should return an empty list when no hub managers exist`() {
      val body = webTestClient.get()
        .uri(path)
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__HUB_MANAGERS__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "empty-list-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should return hub managers when they exist`() {
      createHubManager(id = "48b83e4b-ea09-4ba7-8440-a7e5ed534cb4", name = "test manager 1", hasSignature = true)
      createHubManager(id = "5b13bcd6-3e87-40d8-918a-cbf3b1620516", name = "test manager 2", hasSignature = true)
      createHubManager(id = "a91734a6-f6d6-48ca-8bb5-4e56b3d50c1d", name = "test manager 3", hasSignature = false)

      val body = webTestClient.get()
        .uri(path)
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__HUB_MANAGERS__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-hub-managers-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }

    @Test
    fun `it should only return hub managers with signatures`() {
      createHubManager(id = "48b83e4b-ea09-4ba7-8440-a7e5ed534cb4", name = "test manager 1", hasSignature = true)
      createHubManager(id = "5b13bcd6-3e87-40d8-918a-cbf3b1620516", name = "test manager 2", hasSignature = true)
      createHubManager(id = "a91734a6-f6d6-48ca-8bb5-4e56b3d50c1d", name = "test manager 3", hasSignature = false)

      val body = webTestClient.get()
        .uri("$path?hasSignature=true")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__HUB_MANAGERS__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-hub-managers-with-signatures-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }
  }

  @Nested
  @DisplayName("POST /hub-managers")
  inner class CreateHubManager {
    @Test
    fun `it should return 401 if the request is not authenticated`() {
      webTestClient.post()
        .uri(path)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("create-hub-manager-request".loadJson())
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return a 403 if the client does not have a valid role`() {
      webTestClient.post()
        .uri(path)
        .headers(setAuthorisation(roles = listOf()))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("create-hub-manager-request".loadJson())
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should create a hub manager`() {
      webTestClient.post()
        .uri(path)
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__HUB_MANAGERS__RW")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("create-hub-manager-request".loadJson())
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody()
        .jsonPath("$.data.id").exists()
        .jsonPath("$.data.id").isNotEmpty
        .jsonPath("$.data.name").isEqualTo("test manager 1")
    }
  }

  @Nested
  @DisplayName("GET /hub-manager")
  inner class GetHubManager {
    @Test
    fun `it should return 401 if the request is not authenticated`() {
      webTestClient.get()
        .uri("$path/${UUID.randomUUID()}")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return a 403 if the client does not have a valid role`() {
      webTestClient.get()
        .uri("$path/${UUID.randomUUID()}")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus()
        .isForbidden
    }

    @Test
    fun `it should return a 404 if the hub manager does not exist`() {
      webTestClient.get()
        .uri("$path/${UUID.randomUUID()}")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__HUB_MANAGERS__RW")))
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `it should return a hub manager if it exists`() {
      createHubManager(id = "48b83e4b-ea09-4ba7-8440-a7e5ed534cb4", name = "test manager 1", hasSignature = false)

      val body = webTestClient.get()
        .uri("$path/48b83e4b-ea09-4ba7-8440-a7e5ed534cb4")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__HUB_MANAGERS__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .returnResult()
        .responseBody!!

      JSONAssert.assertEquals(
        "get-hub-manager-response".loadJson(),
        String(body, StandardCharsets.UTF_8),
        JSONCompareMode.NON_EXTENSIBLE,
      )
    }
  }

  private fun createHubManager(id: String, name: String, hasSignature: Boolean): HubManager {
    val manager = HubManager(
      id = UUID.fromString(id),
      name = name,
    )

    if (hasSignature) {
      manager.signatureImage = "fake-signature-image".toByteArray()
      manager.signatureImageContentType = "image/png"
    }

    return repo.save(
      manager,
    )
  }
}
