package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.conditions.And
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery

class Query(val selectStatement: SelectStatement) {
  private var condition: Condition? = null

  fun prepare(): AthenaQuery {
    val builder = StringBuilder()

    builder.append(selectStatement)

    condition
      ?.toString()
      ?.takeIf { it.isNotBlank() }
      ?.let { builder.append(" WHERE ").append(it) }

    return AthenaQuery(
      queryString = builder.toString(),
      parameters = (condition?.parameters() ?: emptyList()),
    )
  }

  fun where(block: Condition.() -> Unit): Query {
    condition = And().apply(block)
    return this
  }
}
