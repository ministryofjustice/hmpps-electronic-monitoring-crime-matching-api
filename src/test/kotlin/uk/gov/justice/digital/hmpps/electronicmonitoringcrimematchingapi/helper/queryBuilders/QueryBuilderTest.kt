package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.queryBuilders

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Table
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.functions.AthenaFunctions
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

object TestTable : Table("test_table") {
  val testColumn1 = integer("test_column_1")
  val testColumn2 = integer("test_column_2")
  val testDateColumn = date("test_date_column")
}

@ActiveProfiles("test")
class QueryBuilderTest {

  @Test
  fun `it should build a select all query`() {
    val query = TestTable
      .selectAll()
      .prepare()

    assertThat(query.queryString).isEqualTo("SELECT test_column_1, test_column_2, test_date_column FROM test_table")
    assertThat(query.parameters).isEqualTo(emptyList<String>().toTypedArray())
  }

  @Test
  fun `it should build a select all query with a where condition`() {
    val query = TestTable
      .selectAll()
      .where {
        TestTable.testColumn1 eq 1
      }
      .prepare()

    assertThat(query.queryString).isEqualTo("SELECT test_column_1, test_column_2, test_date_column FROM test_table WHERE test_column_1 = ?")
    assertThat(query.parameters).isEqualTo(listOf("1").toTypedArray())
  }

  @Test
  fun `it should build a select all query with two where conditions`() {
    val query = TestTable
      .selectAll()
      .where {
        TestTable.testColumn1 eq 1
        TestTable.testColumn2 eq 1
      }
      .prepare()

    assertThat(query.queryString).isEqualTo("SELECT test_column_1, test_column_2, test_date_column FROM test_table WHERE (test_column_1 = ? AND test_column_2 = ?)")
    assertThat(query.parameters).isEqualTo(listOf("1", "1").toTypedArray())
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("data")
  fun `it should dynamically build the query using nullable fields`(testValue: Int?, expectedSQL: String, expectedParams: List<String>) {
    val query = TestTable
      .selectAll()
      .where {
        testValue?.let {
          TestTable.testColumn1 eq it
        }
      }
      .prepare()

    assertThat(query.queryString).isEqualTo(expectedSQL)
    assertThat(query.parameters).isEqualTo(expectedParams.toTypedArray())
  }

  @Test
  fun `it should build a query with a gte conditions`() {
    val query = TestTable
      .selectAll()
      .where {
        TestTable.testColumn1 gte 1
      }
      .prepare()

    assertThat(query.queryString).isEqualTo("SELECT test_column_1, test_column_2, test_date_column FROM test_table WHERE test_column_1 >= ?")
    assertThat(query.parameters).isEqualTo(listOf("1").toTypedArray())
  }

  @Test
  fun `it should build a query using gte date condition`() {
    val date = ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 1, 0, 0), ZoneOffset.UTC)
    val query = TestTable
      .selectAll()
      .where {
        TestTable.testDateColumn gte AthenaFunctions.fromIso8601Timestamp(date)
      }
      .prepare()

    assertThat(query.queryString).isEqualTo("SELECT test_column_1, test_column_2, test_date_column FROM test_table WHERE test_date_column >= from_iso8601_timestamp(?)")
    assertThat(query.parameters).isEqualTo(listOf("'2020-01-01T01:00Z'").toTypedArray())
  }

  @Test
  fun `it should build a query with a lte conditions`() {
    val query = TestTable
      .selectAll()
      .where {
        TestTable.testColumn1 lte 1
      }
      .prepare()

    assertThat(query.queryString).isEqualTo("SELECT test_column_1, test_column_2, test_date_column FROM test_table WHERE test_column_1 <= ?")
    assertThat(query.parameters).isEqualTo(listOf("1").toTypedArray())
  }

  @Test
  fun `it should build a query using lte date condition`() {
    val date = ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 1, 0, 0), ZoneOffset.UTC)
    val query = TestTable
      .selectAll()
      .where {
        TestTable.testDateColumn lte AthenaFunctions.fromIso8601Timestamp(date)
      }
      .prepare()

    assertThat(query.queryString).isEqualTo("SELECT test_column_1, test_column_2, test_date_column FROM test_table WHERE test_date_column <= from_iso8601_timestamp(?)")
    assertThat(query.parameters).isEqualTo(listOf("'2020-01-01T01:00Z'").toTypedArray())
  }

  companion object {
    @JvmStatic
    fun data() = listOf(
      Arguments.of(null, "SELECT test_column_1, test_column_2, test_date_column FROM test_table", emptyList<String>()),
      Arguments.of(1, "SELECT test_column_1, test_column_2, test_date_column FROM test_table WHERE test_column_1 = ?", listOf("1")),
    )
  }
}
