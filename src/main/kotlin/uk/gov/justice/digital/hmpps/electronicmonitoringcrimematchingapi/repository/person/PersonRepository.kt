package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClientInterface
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaHelper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.JoinType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SqlQueryBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaPersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonsQueryCriteria

@Service
class PersonRepository(
  val athenaClient: EmDatastoreClientInterface,
) {

  fun getPersonsQueryResults(queryExecutionId: String): List<AthenaPersonDto> {
    val athenaResponse = athenaClient.getQueryResult(queryExecutionId)
    return AthenaHelper.Companion.mapTo<AthenaPersonDto>(athenaResponse)
  }

  fun getPersonsQueryId(personsQueryCriteria: PersonsQueryCriteria): String {
    val personQuery = SqlQueryBuilder("allied_mdss_test_20250714014447.person", "p")
      .addFields(
        listOf(
          "p.person_id",
          "p.person_name",
          "pdw.u_id_nomis",
          "pdws.u_dob",
          "csm.zip",
          "csm.city",
          "csm.street",
        ),
      )
      .addJoin(
        "serco_servicenow_test.x_serg2_ems_csm_profile_device_wearer pdw",
        "p.person_name = pdw.u_id_device_wearer",
        JoinType.INNER,
      )
      .addJoin(
        "serco_servicenow_test.csm_consumer csm",
        "pdw.consumer__value = csm.sys_id",
        JoinType.INNER,
      )
      .addJoin(
        "serco_servicenow_test.x_serg2_ems_csm_profile_sensitive pdws",
        "csm.sys_id = pdws.consumer__value",
        JoinType.INNER,
      )
      .addLikeFilter("p.person_name", personsQueryCriteria.personName)
      .addLikeFilter("pdw.u_id_nomis", personsQueryCriteria.nomisId)

    if (personsQueryCriteria.includeDeviceActivations) {
      personQuery
        .addFields(
          listOf(
            "da.device_id",
            "da.device_activation_id",
            "da.device_activation_date",
            "da.device_deactivation_date",
          ),
        )
        .addJoin("allied_mdss_test_20250714014447.device_activation da", "p.person_id = da.person_id", JoinType.INNER)
        .addLikeFilterCast("da.device_id", personsQueryCriteria.deviceId)
    }

    val query = personQuery.build()

    return athenaClient.getQueryExecutionId(query)
  }

  fun getPersonById(id: Long): AthenaPersonDto {
    val queryExecutionId = athenaClient.getQueryExecutionId(
      SqlQueryBuilder("allied_mdss_test_20250714014447.person", "p")
        .addFields(
          listOf(
            "p.person_id",
            "p.person_name",
            "pdw.u_id_nomis",
            "pdws.u_dob",
            "csm.zip",
            "csm.city",
            "csm.street",
          ),
        )
        .addJoin(
          "serco_servicenow_test.x_serg2_ems_csm_profile_device_wearer pdw",
          "p.person_name = pdw.u_id_device_wearer",
          JoinType.INNER,
        )
        .addJoin(
          "serco_servicenow_test.csm_consumer csm",
          "pdw.consumer__value = csm.sys_id",
          JoinType.INNER,
        )
        .addJoin(
          "serco_servicenow_test.x_serg2_ems_csm_profile_sensitive pdws",
          "csm.sys_id = pdws.consumer__value",
          JoinType.INNER,
        )
        .addFilter("p.person_id", id)
        .build(),
    )
    val queryResult = athenaClient.getQueryResult(queryExecutionId)
    val persons = AthenaHelper.Companion.mapTo<AthenaPersonDto>(queryResult)

    if (persons.isEmpty()) {
      throw EntityNotFoundException("No person found with id: $id")
    }

    return persons.first()
  }
}
