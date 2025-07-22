package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import io.zeko.db.sql.dsl.like
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaSubjectQuery

class SubjectsQueryBuilder(
  override val databaseName: String,
) : SqlQueryBuilder(
  databaseName,
  "person",
  arrayOf(
    "person_id",
    "person_name",
    "address",
    "date_of_birth",
    "device_id",
    "nomis_id",
    "order_start_date",
    "order_end_date",
    "tag_start_date",
    "tag_end_date",
  ),
) {

  fun withName(value: String?): SubjectsQueryBuilder {
    if (value.isNullOrBlank()) {
      return this
    }

    values.add("'%$value%'")
    whereClauses.put("person_name", "person_name" like "'%$value%'")
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
