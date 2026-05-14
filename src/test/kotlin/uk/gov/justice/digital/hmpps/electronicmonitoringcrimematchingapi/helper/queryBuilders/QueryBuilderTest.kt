package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.queryBuilders

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Table
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.eq

object TestTable : Table("test_table") {
  val test_column_1 = integer("test_column_1")
  val test_column_2 = integer("test_column_2")
}

@ActiveProfiles("test")
class QueryBuilderTest {

  @Test
  fun `it should build a select all query`() {
    val query = TestTable
      .selectAll()
      .prepareSQL()

    assertThat(query).isEqualTo("SELECT test_column_1, test_column_2 FROM test_table")
  }

  @Test
  fun `it should build a select all query with an andWhere condition`() {
    val query = TestTable
      .selectAll()
      .apply {
        andWhere {
          TestTable.test_column_1 eq 1
        }
      }
      .prepareSQL()

    assertThat(query).isEqualTo("SELECT test_column_1, test_column_2 FROM test_table WHERE test_column_1 = ?")
  }

  @Test
  fun `it should build a select all query with two andWhere conditions`() {
    val query = TestTable
      .selectAll()
      .apply {
        andWhere {
          TestTable.test_column_1 eq 1
        }
        andWhere {
          TestTable.test_column_2 eq 1
        }
      }
      .prepareSQL()

    assertThat(query).isEqualTo("SELECT test_column_1, test_column_2 FROM test_table WHERE test_column_1 = ? AND test_column_2 = ?")
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("data")
  fun `it should dynamically build the query using nullable fields`(testValue: Int?, expectedSQL: String) {
    val query = TestTable
      .selectAll()
      .apply {
        testValue?.let {
          andWhere {
            TestTable.test_column_1 eq it
          }
        }
      }
      .prepareSQL()

    assertThat(query).isEqualTo(expectedSQL)
  }

  companion object {
    @JvmStatic
    fun data() = listOf(
      Arguments.of(null, "SELECT test_column_1, test_column_2 FROM test_table"),
      Arguments.of(1, "SELECT test_column_1, test_column_2 FROM test_table WHERE test_column_1 = ?"),
    )
  }

}