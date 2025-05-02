package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClientInterface
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaHelper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SubjectInformationQueryBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.AthenaSubjectInformationDTO

@Service
class SubjectInformationRepository(
  @Autowired val athenaClient: EmDatastoreClientInterface,
  @Value("\${services.athena.database}")
  var athenaDatabase: String = "unknown_database",
) {
  fun getSubjectInformation(legacySubjectId: String): AthenaSubjectInformationDTO {
    val keyOrderInformationQuery = SubjectInformationQueryBuilder(athenaDatabase)
      .withLegacySubjectId(legacySubjectId)
      .build()

    val athenaResponse = athenaClient.getQueryResult(keyOrderInformationQuery)

    val result = AthenaHelper.mapTo<AthenaSubjectInformationDTO>(athenaResponse)

    return result.first()
  }
}
