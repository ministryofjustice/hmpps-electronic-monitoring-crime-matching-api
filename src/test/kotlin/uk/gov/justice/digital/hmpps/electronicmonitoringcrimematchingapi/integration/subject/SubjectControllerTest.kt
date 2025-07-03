package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.subject

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.mocks.MockEmDatastoreClient
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.Subject

@ActiveProfiles("integration")
class SubjectControllerTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /subjects")
  inner class GetSubjects {
    @Test
    fun `it should return subjects when valid search criteria is provided`() {
      MockEmDatastoreClient.addResponseFile("successfulSubjectSearchResponse")
      MockEmDatastoreClient.addResponseFile("successfulGetQueryExecutionIdResponse")

      webTestClient.get()
        .uri("/subjects?name=John&nomisId=12345")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList(Subject::class.java)
        .hasSize(1)
    }
  }

  @Test
  fun `it should fail with bad request when invalid search criteria is provided`() {
    webTestClient.get()
      .uri("/subjects")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus()
      .isBadRequest
  }
}
