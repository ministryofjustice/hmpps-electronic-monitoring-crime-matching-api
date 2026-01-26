package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.datastore.DatastoreProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.JoinType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SqlQueryBuilder

class GetPersonByIdQueryBuilder : SqlQueryBuilder {
  constructor(
    datastoreProperties: DatastoreProperties,
    id: Long,
  ) : super("${datastoreProperties.mdssDatabase}.person", "p") {
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
        "${datastoreProperties.fmsDatabase}.x_serg2_ems_csm_profile_device_wearer pdw",
        "p.person_name = pdw.u_id_device_wearer",
        JoinType.LEFT,
      )
      .addJoin(
        "${datastoreProperties.fmsDatabase}.csm_consumer csm",
        "pdw.consumer = csm.sys_id",
        JoinType.LEFT,
      )
      .addJoin(
        "${datastoreProperties.fmsDatabase}.x_serg2_ems_csm_profile_sensitive pdws",
        "csm.sys_id = pdws.consumer",
        JoinType.LEFT,
      )
      .addFilter("p.person_id", id)
  }
}
