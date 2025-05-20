package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectInformation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectSearchCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.SubjectSearchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.SubjectInformationRepository

@Service
class SubjectService(
  @Autowired val subjectSearchRepository: SubjectSearchRepository,
  //TODO Remove the below and replace with search repo
  @Autowired val subjectInformationRepository: SubjectInformationRepository,
) {
  fun checkAvailability(): Boolean {
    try {
      subjectSearchRepository.listLegacyIds()
    } catch (_: Exception) {
      return false
    }

    return true
  }

  fun getSubjectInformation(legacySubjectId: String): SubjectInformation {
    val subjectInformation = subjectInformationRepository.getSubjectInformation(legacySubjectId)

    return SubjectInformation(subjectInformation.legacySubjectId, subjectInformation.name)
  }

  fun searchSubjects(subjectSearchCriteria: SubjectSearchCriteria): List<SubjectInformation> {
    val results = subjectSearchRepository.searchSubjects(subjectSearchCriteria)
    return results.map { result -> SubjectInformation(result) }
  }

}
