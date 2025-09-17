package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.JoinType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SqlQueryBuilder

class GetPersonByIdQueryBuilder : SqlQueryBuilder {
  constructor(id: Long) : super("allied_mdss_test_20250714014447.person", "p") {
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
      .addFilter("p.person_id", id)
  }
}
