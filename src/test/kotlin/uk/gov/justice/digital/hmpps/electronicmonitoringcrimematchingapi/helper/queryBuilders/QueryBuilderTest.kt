package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.queryBuilders

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.queryBuilders.TestTable.testColumn1
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.queryBuilders.TestTable.testColumn2
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.queryBuilders.TestTable.testDateColumn
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.JoinType
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

object OtherTable : Table("other_table") {
  val testColumn1 = integer("test_column_1")
}

object AnotherTable : Table("another_table") {
  val testColumn1 = integer("test_column_1")
}

@ActiveProfiles("test")
class QueryBuilderTest {

  @Test
  fun `it should build a select all query`() {
    val query = TestTable
      .selectAll()
      .prepare()

    assertThat(query.queryString).isEqualTo("SELECT * FROM test_table")
    assertThat(query.parameters).isEqualTo(emptyList<String>().toTypedArray())
  }

  @Test
  fun `it should build a select query`() {
    val query = TestTable
      .select(
        TestTable.testColumn1,
      )
      .prepare()

    assertThat(query.queryString).isEqualTo("SELECT test_table.test_column_1 FROM test_table")
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

    assertThat(query.queryString).isEqualTo("SELECT * FROM test_table WHERE test_table.test_column_1 = ?")
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

    assertThat(query.queryString).isEqualTo("SELECT * FROM test_table WHERE (test_table.test_column_1 = ? AND test_table.test_column_2 = ?)")
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
  fun `it should build a query with a null check`() {
    val query = TestTable
      .selectAll()
      .where { testColumn1 eq null }
      .prepare()

    assertThat(query.queryString).isEqualTo("SELECT * FROM test_table WHERE test_table.test_column_1 is NULL")
    assertThat(query.parameters).isEqualTo(emptyList<String>().toTypedArray())
  }

  @Test
  fun `it should build a query with a gte conditions`() {
    val query = TestTable
      .selectAll()
      .where {
        TestTable.testColumn1 gte 1
      }
      .prepare()

    assertThat(query.queryString).isEqualTo("SELECT * FROM test_table WHERE test_table.test_column_1 >= ?")
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

    assertThat(query.queryString).isEqualTo("SELECT * FROM test_table WHERE test_table.test_date_column >= from_iso8601_timestamp(?)")
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

    assertThat(query.queryString).isEqualTo("SELECT * FROM test_table WHERE test_table.test_column_1 <= ?")
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

    assertThat(query.queryString).isEqualTo("SELECT * FROM test_table WHERE test_table.test_date_column <= from_iso8601_timestamp(?)")
    assertThat(query.parameters).isEqualTo(listOf("'2020-01-01T01:00Z'").toTypedArray())
  }

  @Test
  fun `it should build a join query`() {
    val query = TestTable
      .join(OtherTable, JoinType.INNER) {
        TestTable.testColumn1 eq OtherTable.testColumn1
      }
      .join(AnotherTable, JoinType.LEFT) {
        TestTable.testColumn1 eq AnotherTable.testColumn1
      }
      .selectAll()
      .prepare()

    assertThat(query.queryString).isEqualTo("SELECT * FROM test_table INNER JOIN other_table ON test_table.test_column_1 = other_table.test_column_1 LEFT JOIN another_table ON test_table.test_column_1 = another_table.test_column_1")
    assertThat(query.parameters).isEqualTo(emptyList<String>().toTypedArray())
  }

  @Test
  fun `it should build a join query using aliases`() {
    val tt = TestTable.aliased("tt")
    val ot = OtherTable.aliased("ot")
    val at = AnotherTable.aliased("at")

    val query = tt
      .join(ot, JoinType.INNER) {
        tt[TestTable.testColumn1] eq ot[OtherTable.testColumn1]
      }
      .join(at, JoinType.LEFT) {
        tt[TestTable.testColumn1] eq at[AnotherTable.testColumn1]
      }
      .select(

        tt[TestTable.testColumn1],
        ot[TestTable.testColumn1],
        at[TestTable.testColumn1],

      )
      .prepare()

    assertThat(query.queryString).isEqualTo("SELECT tt.test_column_1, ot.test_column_1, at.test_column_1 FROM test_table tt INNER JOIN other_table ot ON tt.test_column_1 = ot.test_column_1 LEFT JOIN another_table at ON tt.test_column_1 = at.test_column_1")
    assertThat(query.parameters).isEqualTo(emptyList<String>().toTypedArray())
  }

  companion object {
    @JvmStatic
    fun data() = listOf(
      Arguments.of(null, "SELECT * FROM test_table", emptyList<String>()),
      Arguments.of(1, "SELECT * FROM test_table WHERE test_table.test_column_1 = ?", listOf("1")),
    )
  }
}
