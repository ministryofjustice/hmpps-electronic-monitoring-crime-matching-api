package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaSubjectSearchQuery

class ListSubjectInformationQueryBuilder(
  override val databaseName: String,
) : SqlQueryBuilder(
    databaseName,
    "order_details",
    arrayOf(
      "legacy_subject_id",
  ),
) {
  fun build(): AthenaSubjectSearchQuery = AthenaSubjectSearchQuery(getSQL(), values.toTypedArray())
}