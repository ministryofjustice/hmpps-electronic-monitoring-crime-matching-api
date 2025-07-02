package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.subject

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectsQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.Subject
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.subject.SubjectRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.subject.SubjectsQueryCacheRepository
import java.time.ZonedDateTime

@Service
class SubjectService(
  @Autowired val subjectsQueryCacheRepository: SubjectsQueryCacheRepository,
  @Autowired val subjectRepository: SubjectRepository,
) {

  @Transactional
  fun getSubjectsQueryResults(subjectsQueryCriteria: SubjectsQueryCriteria, user: String): List<Subject> {
    //Check cache for existing query
    var queryExecutionId = subjectsQueryCacheRepository.findByNomisIdAndSubjectNameAndCreatedAtAfter(
      subjectsQueryCriteria.nomisId,
      subjectsQueryCriteria.name,
      ZonedDateTime.now().minusDays(1)
    )?.queryExecutionId

    if (queryExecutionId == null) {
      //If it doesn't exist, execute the Athena query and return id
      queryExecutionId = subjectRepository.getSubjectsQueryId(subjectsQueryCriteria)

      //Save query to cache
      subjectsQueryCacheRepository.save(
        SubjectsQuery(
          nomisId = subjectsQueryCriteria.nomisId,
          subjectName = subjectsQueryCriteria.name,
          queryExecutionId = queryExecutionId,
          queryOwner = user
        )
      )
    }

    //Get subject results from Athena using query id
    val res = subjectRepository.getSubjectsQueryResults(queryExecutionId)
    return res.map { result -> Subject(result) }
  }

  fun checkAvailability(): Boolean {
    try {
      subjectRepository.listLegacyIds()
    } catch (_: Exception) {
      return false
    }

    return true
  }
}