package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.subject

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.mocks.MockEmDatastoreClient
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.Subject
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectsQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.subject.SubjectsQueryCacheRepository
import java.time.ZonedDateTime

@ActiveProfiles("integration")
class SubjectControllerTest : IntegrationTestBase() {

  @Autowired
  protected lateinit var subjectsQueryCacheRepository: SubjectsQueryCacheRepository

  @BeforeEach
  fun setup() {
    subjectsQueryCacheRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /subjects")
  inner class GetSubjects {
    @Test
    fun `it should return subjects when valid search criteria is provided and new query saved to cache`() {
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

      val dbResult = subjectsQueryCacheRepository.findByNomisIdAndSubjectNameAndCreatedAtAfter("12345", "John", ZonedDateTime.now().minusDays(2))
      assertThat(dbResult).isNotNull
    }
  }

  @Test
  fun `it should return subjects when valid search criteria is provided and reuses existing query from cache`() {
    MockEmDatastoreClient.addResponseFile("successfulSubjectSearchResponse")
    MockEmDatastoreClient.addResponseFile("successfulGetQueryExecutionIdResponse")

    subjectsQueryCacheRepository.save(
      SubjectsQuery(
        nomisId = "12345",
        subjectName = "John",
        queryExecutionId = "queryId",
        queryOwner = "user1",
      ),
    )

    webTestClient.get()
      .uri("/subjects?name=John&nomisId=12345")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus()
      .isOk
      .expectBodyList(Subject::class.java)
      .hasSize(1)

    val dbResult = subjectsQueryCacheRepository.findByNomisIdAndSubjectNameAndCreatedAtAfter("12345", "John", ZonedDateTime.now().minusDays(2))
    assertThat(dbResult).isNotNull
    assertThat(dbResult?.queryExecutionId).isEqualTo("queryId")
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
