package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

class NotFoundTest : IntegrationTestBase() {

  @Test
  fun `Resources that aren't found should return 404 - test of the exception handler`() {
    val response = webTestClient.get().uri("/some-url-not-found")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isNotFound
      .expectBody(ErrorResponse::class.java)
      .returnResult()
      .responseBody!!

    assertThat(response).isEqualTo(
      ErrorResponse(
        status = 404,
        userMessage = "No resource found failure: No static resource some-url-not-found for request '/some-url-not-found'.",
        developerMessage = "No static resource some-url-not-found for request '/some-url-not-found'.",
      ),
    )
  }
}
