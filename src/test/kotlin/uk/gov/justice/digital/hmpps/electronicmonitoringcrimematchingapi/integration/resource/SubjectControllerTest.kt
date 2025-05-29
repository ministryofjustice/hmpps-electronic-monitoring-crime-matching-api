package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.QueryExecutionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectInformation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectSearchCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.subject.SubjectController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.SubjectService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.AuditService

@ActiveProfiles("test")
class SubjectControllerTest {
  private lateinit var subjectService: SubjectService
  private lateinit var auditService: AuditService
  private lateinit var controller: SubjectController
  private lateinit var authentication: Authentication

  @BeforeEach
  fun setup() {
    authentication = mock(Authentication::class.java)
    whenever(authentication.name).thenReturn("MOCK_AUTH_USER")
    subjectService = mock(SubjectService::class.java)
    auditService = mock(AuditService::class.java)
    controller = SubjectController(subjectService, auditService)
  }

  @Nested
  inner class SearchSubjects {
    @Test
    fun `calls SubjectService for getQueryExecutionId`() {
      val subjectSearchCriteria = SubjectSearchCriteria(name = "John", nomisId = "12345")
      val queryExecutionId = "query-execution-id"

      val expectedResult = QueryExecutionResponse(
        queryExecutionId = queryExecutionId,
      )

      whenever(subjectService.getQueryExecutionId(subjectSearchCriteria)).thenReturn(queryExecutionId)

      val result = controller.executeSearch(authentication, subjectSearchCriteria)
      assertThat(result.body).isNotNull
      assertThat(result.body).isEqualTo(expectedResult)
    }
  }

  @Nested
  inner class GetSubjectSearchResult {
    @Test
    fun `calls SubjectService for getSubjectSearchResults`() {
      val queryExecutionId = "query-execution-id"

      val expectedResult = listOf(SubjectInformation(
        "12345",
        "John",
        "2000-05-29T10:57:06.932277",
        "2 Green Grove",
        "2024-05-29T10:57:06.932277",
        "2026-05-29T10:57:06.932277",
        "87654",
        "2024-05-29T10:57:06.932277",
        "2026-05-29T10:57:06.932277",
      ))

      whenever(subjectService.getSubjectSearchResults(queryExecutionId)).thenReturn(expectedResult)

      val result = controller.getSubjectSearchResults(queryExecutionId)
      assertThat(result.body).isNotNull
      assertThat(result.body).isEqualTo(expectedResult)
    }
  }
}
