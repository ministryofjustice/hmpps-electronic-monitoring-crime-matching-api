package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.subject

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.athena.model.ResultSet
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClientInterface
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaHelper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.ListSubjectInformationQueryBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SubjectsQueryBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaSubjectDTO
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectsQueryCriteria

@Service
class SubjectRepository(
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

    val result = AthenaHelper.Companion.mapTo<SubjectId>(athenaResponse)

    return result.map { it.legacySubjectId }
  }

  fun getSubjectsQueryResults(queryExecutionId: String): List<AthenaSubjectDTO> {
    val athenaResponse = athenaClient.getQueryResult(queryExecutionId)
    return AthenaHelper.Companion.mapTo<AthenaSubjectDTO>(athenaResponse)
  }

  fun getSubjectsQueryId(subjectsQueryCriteria: SubjectsQueryCriteria): String {
    val subjectSearchQuery = SubjectsQueryBuilder(athenaDatabase)
      .withNomisId(subjectsQueryCriteria.nomisId)
      .withName(subjectsQueryCriteria.name)
      .build()

    return athenaClient.getQueryExecutionId(subjectSearchQuery)
  }
}
