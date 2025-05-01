package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

import io.zeko.db.sql.Query
import io.zeko.db.sql.QueryBlock
import org.apache.commons.lang3.StringUtils.isAlphanumeric
import org.apache.commons.lang3.StringUtils.isAlphanumericSpace

open class SqlQueryBuilder(
  open val databaseName: String,
  open val tableName: String,
  private val fields: Array<String>,
) {
  protected val whereClauses: MutableMap<String, QueryBlock> = mutableMapOf<String, QueryBlock>()
  protected val values: MutableList<String> = mutableListOf<String>()

  protected fun getSQL(): String {
    val query = Query()
      .fields(*fields)
      .from("$databaseName.$tableName")

    whereClauses.forEach {
      query.where(it.value)
    }

    return query.toSql()
  }

  @Suppress("SameParameterValue")
  protected fun validateAlphanumeric(value: String?, field: String) {
    if (value.isNullOrBlank()) {
      return
    }

    if (!isAlphanumeric(value)) {
      throw IllegalArgumentException("$field must only contain alphanumeric characters and spaces")
    }
  }
}
