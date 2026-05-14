package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.queryBuilders

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Table
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.fromIso8601Timestamp
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
      .prepareSQL()

    assertThat(query).isEqualTo("SELECT test_column_1, test_column_2, test_date_column FROM test_table")
  }

  @Test
  fun `it should build a select all query with a where condition`() {
    val query = TestTable
      .selectAll()
      .where {
        TestTable.testColumn1 eq 1
      }
      .prepareSQL()

    assertThat(query).isEqualTo("SELECT test_column_1, test_column_2, test_date_column FROM test_table WHERE test_column_1 = ?")
  }

  @Test
  fun `it should build a select all query with two where conditions`() {
    val query = TestTable
      .selectAll()
      .where {
        TestTable.testColumn1 eq 1
        TestTable.testColumn2 eq 1
      }
      .prepareSQL()

    assertThat(query).isEqualTo("SELECT test_column_1, test_column_2, test_date_column FROM test_table WHERE (test_column_1 = ? AND test_column_2 = ?)")
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("data")
  fun `it should dynamically build the query using nullable fields`(testValue: Int?, expectedSQL: String) {
    val query = TestTable
      .selectAll()
      .where {
        testValue?.let {
          TestTable.testColumn1 eq it
        }
      }
      .prepareSQL()

    assertThat(query).isEqualTo(expectedSQL)
  }

  @Test
  fun `it should build a query using gte date filter`() {
    val date = ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 1, 0, 0), ZoneOffset.UTC)
    val query = TestTable
      .selectAll()
      .where {
        TestTable.testDateColumn gte fromIso8601Timestamp(date)
      }
      .prepareSQL()

    assertThat(query).isEqualTo("SELECT test_column_1, test_column_2, test_date_column FROM test_table WHERE test_date_column >= from_iso8601_timestamp(?)")
  }

  companion object {
    @JvmStatic
    fun data() = listOf(
      Arguments.of(null, "SELECT test_column_1, test_column_2, test_date_column FROM test_table"),
      Arguments.of(1, "SELECT test_column_1, test_column_2, test_date_column FROM test_table WHERE test_column_1 = ?"),
    )
  }
}
