package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.AthenaRole
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.SubjectInformation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.SearchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.SubjectInformationRepository

@Service
class SubjectService(
  @Autowired val searchRepository: SearchRepository,
  @Autowired val subjectInformationRepository: SubjectInformationRepository,
) {
  fun checkAvailability(role: AthenaRole): Boolean {
    try {
      searchRepository.listLegacyIds(role)
    } catch (_: Exception) {
      return false
    }

    return true
  }

  fun getSubjectInformation(legacySubjectId: String, role: AthenaRole): SubjectInformation {
    val subjectInformation = subjectInformationRepository.getSubjectInformation(legacySubjectId, role)

    return SubjectInformation(subjectInformation.legacySubjectId, subjectInformation.name)
  }
}
