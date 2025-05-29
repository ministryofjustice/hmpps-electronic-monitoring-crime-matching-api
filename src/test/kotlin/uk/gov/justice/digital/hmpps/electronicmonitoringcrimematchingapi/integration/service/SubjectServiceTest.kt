package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaSubjectInformationDTO
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectInformation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectSearchCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.SubjectSearchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.SubjectService


class SubjectServiceTest {
  private lateinit var subjectSearchRepository: SubjectSearchRepository
  private lateinit var service: SubjectService

  @BeforeEach
  fun setup() {
    subjectSearchRepository = mock(SubjectSearchRepository::class.java)
    service = SubjectService(subjectSearchRepository)
  }

  @Nested
  inner class CheckAvailability {
    @Test
    fun `returns true if athena is available`() {
      whenever(subjectSearchRepository.listLegacyIds()).thenReturn(listOf())

      val result = service.checkAvailability()

      assertThat(result).isTrue
    }

    @Test
    fun `returns false if athena is unavailable`() {
      whenever(subjectSearchRepository.listLegacyIds()).thenThrow(NullPointerException())

      val result = service.checkAvailability()

      assertThat(result).isFalse
    }
  }

  @Nested
  inner class GetQueryExecutionId {
    @Test
    fun `returns query execution Id when submitting search criteria`() {
      val subjectSearchCriteria = SubjectSearchCriteria(name = "John", nomisId = "12345")
      val queryExecutionId = "query-execution-id"
      whenever(subjectSearchRepository.searchSubjects(subjectSearchCriteria)).thenReturn(queryExecutionId)

      val result = service.getQueryExecutionId(subjectSearchCriteria)

      assertThat(result).isEqualTo(queryExecutionId)
    }
  }

  @Nested
  inner class GetSubjectSearchResults {
    @Test
    fun `returns list of subjects when submitting a query execution Id`() {
      val queryExecutionId = "query-execution-id"
      val expectedResult = listOf(AthenaSubjectInformationDTO(
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
        "",
      ))

      whenever(subjectSearchRepository.getSubjectSearchResults(queryExecutionId)).thenReturn(expectedResult)

      val result = service.getSubjectSearchResults(queryExecutionId)

      assertThat(result).isInstanceOf(List::class.java)
      assertThat(result.count()).isEqualTo(1)
      assertThat(result.first()).isInstanceOf(SubjectInformation::class.java)
    }
  }
}