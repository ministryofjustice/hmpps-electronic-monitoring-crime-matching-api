package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.subject

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaSubjectDTO
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.Subject
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectsQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.subject.SubjectRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.subject.SubjectsQueryCacheRepository

class SubjectsServiceTest {
  private lateinit var queryCacheRepository: SubjectsQueryCacheRepository
  private lateinit var subjectRepository: SubjectRepository
  private lateinit var service: SubjectService

  @BeforeEach
  fun setup() {
    subjectRepository = Mockito.mock(SubjectRepository::class.java)
    queryCacheRepository = Mockito.mock(SubjectsQueryCacheRepository::class.java)
    service = SubjectService(queryCacheRepository, subjectRepository)
  }

  @Nested
  @DisplayName("CheckAvailability")
  inner class CheckAvailability {
    @Test
    fun `it should return true if athena is available`() {
      whenever(subjectRepository.listLegacyIds()).thenReturn(listOf())

      val result = service.checkAvailability()

      Assertions.assertThat(result).isTrue
    }

    @Test
    fun `it should return false if athena is unavailable`() {
      whenever(subjectRepository.listLegacyIds()).thenThrow(NullPointerException())

      val result = service.checkAvailability()

      Assertions.assertThat(result).isFalse
    }
  }

  @Nested
  @DisplayName("getSubjectQueryResults")
  inner class GetSubjectQueryResults {
    @Test
    fun `it should return list of subjects when submitting a matching query`() {
      val subjectsQueryCriteria = SubjectsQueryCriteria(name = "John", nomisId = "12345")
      val queryExecutionId = "query-execution-id"
      val expectedResult = listOf(
        AthenaSubjectDTO(
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
        ),
      )

      val subjectsQuery = SubjectsQuery(1, "", "", queryExecutionId, "")

      whenever(
        queryCacheRepository.findByNomisIdAndSubjectNameAndCreatedAtAfter(
          eq("12345"),
          eq("John"),
          any(),
        ),
      ).thenReturn(subjectsQuery)
      whenever(subjectRepository.getSubjectsQueryResults(queryExecutionId)).thenReturn(expectedResult)

      val result = service.getSubjectsQueryResults(subjectsQueryCriteria, "")

      Assertions.assertThat(result).isInstanceOf(List::class.java)
      Assertions.assertThat(result.count()).isEqualTo(1)
      Assertions.assertThat(result.first()).isInstanceOf(Subject::class.java)
    }

    @Test
    fun `it should return list of subjects when submitting a new query`() {
      val subjectsQueryCriteria = SubjectsQueryCriteria(name = "John", nomisId = "12345")
      val queryExecutionId = "query-execution-id"
      val expectedResult = listOf(
        AthenaSubjectDTO(
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
        ),
      )

      whenever(
        queryCacheRepository.findByNomisIdAndSubjectNameAndCreatedAtAfter(
          eq("12345"),
          eq("John"),
          any(),
        ),
      ).thenReturn(null)
      whenever(subjectRepository.getSubjectsQueryId(subjectsQueryCriteria)).thenReturn(queryExecutionId)
      whenever(subjectRepository.getSubjectsQueryResults(queryExecutionId)).thenReturn(expectedResult)

      val result = service.getSubjectsQueryResults(subjectsQueryCriteria, "")

      Assertions.assertThat(result).isInstanceOf(List::class.java)
      Assertions.assertThat(result.count()).isEqualTo(1)
      Assertions.assertThat(result.first()).isInstanceOf(Subject::class.java)
    }
  }
}
