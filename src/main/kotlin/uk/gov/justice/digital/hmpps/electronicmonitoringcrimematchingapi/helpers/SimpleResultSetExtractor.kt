package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import software.amazon.awssdk.services.athena.model.ResultSet
import software.amazon.awssdk.services.athena.model.Row

class SimpleResultSetExtractor<T>(
  val targetType: Class<T>,
) : AthenaResultSetExtractor<T> {

  override fun extractData(resultSet: ResultSet): List<T> {
    val mapper = getMapper()
    val columnNames = getColumnsNames(resultSet)

    return resultSet
      .rows()
      .drop(1)
      .map { row ->
        mapper.convertValue(
          extractRowDataToJson(row, columnNames),
          targetType,
        )
      }
  }

  fun getMapper(): ObjectMapper = jacksonObjectMapper()
    .findAndRegisterModules()
    .registerKotlinModule()
    .apply {
      propertyNamingStrategy = AthenaHelper.Companion.AlphanumericSnakeCaseStrategy()
    }

  fun getColumnsNames(resultSet: ResultSet): List<String> = resultSet
    .resultSetMetadata()
    .columnInfo()
    .map {
      it.name()
    }

  fun extractRowDataToJson(row: Row, columnNames: List<String>): Map<String, String?> = row
    .data()
    .mapIndexed { i, datum ->
      columnNames[i] to datum.varCharValue()
    }.toMap()
}
