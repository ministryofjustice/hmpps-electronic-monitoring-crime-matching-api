package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import io.zeko.db.sql.dsl.like
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaSubjectQuery

class SubjectsQueryBuilder(
  override val databaseName: String,
) : SqlQueryBuilder(
  databaseName,
  "subject",
  arrayOf(
    "nomis_id",
    "full_name",
    "date_of_birth",
    "address",
    "order_start_date",
    "order_end_date",
    "device_id",
    "tag_period_start_date",
    "tag_period_end_date",
  ),
) {

  fun withName(value: String?): SubjectsQueryBuilder {
    if (value.isNullOrBlank()) {
      return this
    }

    values.add("'%$value%'")
    whereClauses.put("full_name", "full_name" like "'%$value%'")
    return this
  }

  fun withNomisId(value: String?): SubjectsQueryBuilder {
    if (value.isNullOrBlank()) {
      return this
    }

    values.add("'%$value%'")
    whereClauses.put("nomis_id", "nomis_id" like "'%$value%'")
    return this
  }

  fun build(): AthenaSubjectQuery = AthenaSubjectQuery(getSQL(), values.toTypedArray())
}
