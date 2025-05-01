package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.json.JSONObject
import software.amazon.awssdk.services.athena.model.ColumnInfo
import software.amazon.awssdk.services.athena.model.ColumnNullable
import software.amazon.awssdk.services.athena.model.Datum
import software.amazon.awssdk.services.athena.model.ResultSet
import software.amazon.awssdk.services.athena.model.ResultSetMetadata
import software.amazon.awssdk.services.athena.model.Row

class AthenaHelper {
  companion object {
    class AlphanumericSnakeCaseStrategy : PropertyNamingStrategies.NamingBase() {
      override fun translate(input: String?): String? = if (input == null) {
        null
      } else {
        "([A-Z]+|[0-9]+)".toRegex().replace(input) { "_${it.groupValues[1]}".lowercase() }
      }
    }

    fun resultSetFromJson(string: String): ResultSet {
      val parsedJson = JSONObject(string)
      return resultSetFromJson(parsedJson)
    }

    fun resultSetFromJson(jsonData: JSONObject): ResultSet {
      val rows: List<Row> = jsonData
        .getJSONObject("ResultSet")
        .getJSONArray("Rows")
        .map { row ->
          Row.builder()
            .data(
              (row as JSONObject).getJSONArray("Data").map { dataJson ->
                Datum.builder()
                  .varCharValue((dataJson as JSONObject).optString("VarCharValue", null))
                  .build()
              },
            )
            .build()
        }

      val columnInfo: List<ColumnInfo> = jsonData
        .getJSONObject("ResultSet")
        .getJSONObject("ResultSetMetadata")
        .getJSONArray("ColumnInfo")
        .map { columnJson ->
          val column = columnJson as JSONObject
          ColumnInfo.builder()
            .catalogName(column.getString("CatalogName"))
            .schemaName(column.getString("SchemaName"))
            .tableName(column.getString("TableName"))
            .name(column.getString("Name"))
            .label(column.getString("Label"))
            .type(column.getString("Type"))
            .precision(column.getInt("Precision"))
            .scale(column.getInt("Scale"))
            .nullable(ColumnNullable.valueOf(column.getString("Nullable").uppercase()))
            .caseSensitive(column.getBoolean("CaseSensitive"))
            .build()
        }

      val resultSet: ResultSet = ResultSet.builder()
        .rows(rows)
        .resultSetMetadata(
          ResultSetMetadata.builder()
            .columnInfo(columnInfo)
            .build(),
        )
        .build()

      return resultSet
    }

    inline fun <reified T> mapTo(resultSet: ResultSet): List<T> {
      val mapper = jacksonObjectMapper()
        .registerKotlinModule()
        .apply {
          propertyNamingStrategy = AlphanumericSnakeCaseStrategy()
        }

      val columnNames: List<String> = resultSet.resultSetMetadata().columnInfo().map { it.name() }

      val mappedRows: List<Map<String, String?>> = resultSet.rows().drop(1).map { row ->
        row.data().mapIndexed { i, datum ->
          columnNames[i] to datum.varCharValue()
        }.toMap()
      }

      return mappedRows.map { row ->
        mapper.convertValue(row, T::class.java)
      }
    }
  }
}
