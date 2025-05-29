package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.athena.model.ResultSet
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClientInterface
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaHelper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.ListSubjectInformationQueryBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SubjectSearchQueryBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaSubjectInformationDTO
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectSearchCriteria

@Service
class SubjectSearchRepository(
  @Autowired val athenaClient: EmDatastoreClientInterface,
  @Value("\${services.athena.database}")
  var athenaDatabase: String = "unknown_database",
) {

  fun listLegacyIds(): List<String> {
    val athenaQuery = ListSubjectInformationQueryBuilder(athenaDatabase).build()

    val athenaResponse: ResultSet = athenaClient.getQueryResult(athenaQuery)

    data class SubjectId(
      val legacySubjectId: String,
    )

    val result = AthenaHelper.mapTo<SubjectId>(athenaResponse)

    return result.map { it.legacySubjectId }
  }

  fun getSubjectSearchResults(queryExecutionId: String): List<AthenaSubjectInformationDTO> {
    val athenaResponse = athenaClient.getQueryResult(queryExecutionId)
    return AthenaHelper.mapTo<AthenaSubjectInformationDTO>(athenaResponse)
  }

  fun searchSubjects(subjectSearchCriteria: SubjectSearchCriteria): String {
    val subjectSearchQuery = SubjectSearchQueryBuilder(athenaDatabase)
      .withNomisId(subjectSearchCriteria.nomisId)
      .withName(subjectSearchCriteria.name)
      .build()

    return athenaClient.getQueryExecutionId(subjectSearchQuery)
  }
}
