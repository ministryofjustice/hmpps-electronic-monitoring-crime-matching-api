package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import io.zeko.db.sql.dsl.like
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaSubjectQuery

class SubjectSearchQueryBuilder(
  override val databaseName: String,
) : SqlQueryBuilder(
  databaseName,
  "order_details",
  arrayOf(
    "legacy_subject_id",
    "full_name",
  ),
) {

  fun withName(value: String?): SubjectSearchQueryBuilder {
    if (value.isNullOrBlank()) {
      return this
    }

    values.add("UPPER('%$value%')")
    whereClauses.put("full_name", "full_name" like "UPPER('%$value%')")
    return this
  }

  //Using legacySubjectId for now
  fun withNomisId(value: String?): SubjectSearchQueryBuilder {
    if (value.isNullOrBlank()) {
      return this
    }

    values.add("UPPER('%$value%')")
    whereClauses.put("legacy_subject_id", "legacy_subject_id" like "UPPER('%$value%')")
    return this
  }

  fun build(): AthenaSubjectQuery = AthenaSubjectQuery(getSQL(), values.toTypedArray())
}
