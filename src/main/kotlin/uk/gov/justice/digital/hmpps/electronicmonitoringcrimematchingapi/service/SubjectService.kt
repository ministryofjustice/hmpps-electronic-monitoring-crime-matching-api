package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.PageResult
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

  fun getSubjectSearchResults(queryExecutionId: String, page: Int, pageSize: Int): PageResult<SubjectInformation> {
    val results = subjectSearchRepository.getSubjectSearchResults(queryExecutionId)

    if (results.isEmpty()) {
      return PageResult(page, 0, emptyList())
    }

    val mappedResults =  results.map { result -> SubjectInformation(result) }

    val totalResults = mappedResults.size
    val totalPages = (totalResults + pageSize - 1) / pageSize

    if (page > totalPages) {
      return PageResult(page, totalPages, emptyList())
    }

    val fromIndex = (page - 1) * pageSize
    val toIndex = minOf(fromIndex + pageSize, totalResults)

    return PageResult(
      pageNumber = page,
      totalPages = totalPages,
      results = mappedResults.subList(fromIndex, toIndex)
    )
  }
}
