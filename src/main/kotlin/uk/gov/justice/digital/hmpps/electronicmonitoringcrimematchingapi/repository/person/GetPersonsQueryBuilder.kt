package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.JoinType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SqlQueryBuilder

class GetPersonsQueryBuilder : SqlQueryBuilder {
  constructor(personsQueryCriteria: PersonsQueryCriteria) : super("allied_mdss_test_20250714014447.person", "p") {
    this.addFields(
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
      .addLikeFilter("p.person_name", personsQueryCriteria.name)
      .addLikeFilter("pdw.u_id_nomis", personsQueryCriteria.nomisId)

    if (personsQueryCriteria.includeDeviceActivations) {
      this
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
  }
}
