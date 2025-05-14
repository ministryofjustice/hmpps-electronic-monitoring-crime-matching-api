package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SubjectInformation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.SubjectController
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
  inner class GetSubject {
    @Test
    fun `calls SubjectService for getSubjectInformation`() {
      val expectedResponse = SubjectInformation("12345", "testName")

      whenever(subjectService.getSubjectInformation("12345")).thenReturn(expectedResponse)

      val result = controller.getSubject(authentication, "12345")
      assertThat(result.body).isNotNull
      assertThat(result.body).isEqualTo(expectedResponse)
    }
  }
}
