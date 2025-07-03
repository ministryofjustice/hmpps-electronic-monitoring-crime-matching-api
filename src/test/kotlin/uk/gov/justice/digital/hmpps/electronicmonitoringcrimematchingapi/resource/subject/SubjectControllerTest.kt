package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.subject

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.Subject
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.subject.SubjectService
import java.time.LocalDateTime

@ActiveProfiles("test")
class SubjectControllerTest {
  private lateinit var subjectService: SubjectService
  private lateinit var controller: SubjectController
  private lateinit var authentication: Authentication

  @BeforeEach
  fun setup() {
    authentication = Mockito.mock(Authentication::class.java)
    whenever(authentication.name).thenReturn("MOCK_AUTH_USER")
    subjectService = Mockito.mock(SubjectService::class.java)
    controller = SubjectController(subjectService)
  }

  @Nested
  @DisplayName("GetSubjects")
  inner class GetSubject {
    @Test
    fun `it should return subjects when valid criteria passed`() {
      val subjectsQueryCriteria = SubjectsQueryCriteria(name = "John", nomisId = "12345")

      val expectedResult = listOf(
        Subject(
          "1",
          "12345",
          "John",
          LocalDateTime.parse("2000-05-29T10:57:06.932277"),
          "2 Green Grove",
          LocalDateTime.parse("2024-05-29T10:57:06.932277"),
          LocalDateTime.parse("2026-05-29T10:57:06.932277"),
          "87654",
          LocalDateTime.parse("2024-05-29T10:57:06.932277"),
          LocalDateTime.parse("2026-05-29T10:57:06.932277"),
        ),
      )

      whenever(subjectService.getSubjectsQueryResults(subjectsQueryCriteria, authentication.name)).thenReturn(expectedResult)

      val result = controller.getSubjects(authentication, subjectsQueryCriteria)
      Assertions.assertThat(result.body).isNotNull
      Assertions.assertThat(result.body).isEqualTo(expectedResult)
    }
  }
}
