package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.resource.ConnectivityController
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.SubjectService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.AuditService

@ActiveProfiles("test")
class ConnectivityControllerTest {
  private lateinit var subjectService: SubjectService
  private lateinit var auditService: AuditService
  private lateinit var controller: ConnectivityController
  private lateinit var authentication: Authentication

  @BeforeEach
  fun setup() {
    authentication = mock(Authentication::class.java)
    `when`(authentication.name).thenReturn("MOCK_AUTH_USER")
    subjectService = mock(SubjectService::class.java)
    auditService = mock(AuditService::class.java)
    controller = ConnectivityController(subjectService, auditService)
  }

  @Nested
  inner class TestAthenaConnection {
    @Test
    fun `calls SubjectService for checkAvailability`() {
      `when`(subjectService.checkAvailability()).thenReturn(true)
      `when`(authentication.principal).thenReturn("EXPECTED_PRINCIPAL")

      val result = controller.test(authentication)

      assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(result.body).isEqualTo(mapOf("message" to "API Connection successful"))
    }

    @Test
    fun `calls SubjectService for checkAvailability connection unavailable`() {
      `when`(subjectService.checkAvailability()).thenReturn(false)
      `when`(authentication.principal).thenReturn("EXPECTED_PRINCIPAL")

      val result = controller.test(authentication)

      assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(result.body).isEqualTo(mapOf("message" to "API Connection successful, but no access to Athena"))
    }

    @Test
    fun `calls SubjectService for checkAvailability exception thrown`() {
      `when`(subjectService.checkAvailability()).thenThrow(NullPointerException("Failed"))
      `when`(authentication.principal).thenReturn("EXPECTED_PRINCIPAL")

      val result = controller.test(authentication)

      assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(result.body).isEqualTo(mapOf("message" to "Error determining Athena access"))
    }
  }
}
