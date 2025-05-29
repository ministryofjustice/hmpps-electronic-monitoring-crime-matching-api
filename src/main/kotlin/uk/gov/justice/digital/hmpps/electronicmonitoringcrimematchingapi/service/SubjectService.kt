package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectInformation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectSearchCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.SubjectSearchRepository

@Service
class SubjectService(
  @Autowired val subjectSearchRepository: SubjectSearchRepository,
) {
  fun checkAvailability(): Boolean {
    try {
      subjectSearchRepository.listLegacyIds()
    } catch (_: Exception) {
      return false
    }

    return true
  }

  fun getQueryExecutionId(subjectSearchCriteria: SubjectSearchCriteria): String = subjectSearchRepository.searchSubjects(subjectSearchCriteria)

  fun getSubjectSearchResults(queryExecutionId: String): List<SubjectInformation> {
    val results = subjectSearchRepository.getSubjectSearchResults(queryExecutionId)
    return results.map { result -> SubjectInformation(result) }
  }
}
