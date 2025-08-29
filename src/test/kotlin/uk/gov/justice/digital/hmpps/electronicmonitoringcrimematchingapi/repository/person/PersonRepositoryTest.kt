package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClient
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaHelper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaPersonDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person.PersonsQueryCriteria

@ActiveProfiles("test")
class PersonRepositoryTest {
  private lateinit var athenaClient: EmDatastoreClient
  private lateinit var repository: PersonRepository

  @BeforeEach
  fun setup() {
    athenaClient = mock(EmDatastoreClient::class.java)
    repository = PersonRepository(athenaClient)
  }

  @Nested
  @DisplayName("GetPersonsQueryId")
  inner class GetPersonsQueryId {
    @Test
    fun `it should return queryExecutionId`() {
      val personsQueryCriteria = PersonsQueryCriteria(personName = "name", includeDeviceActivations = true)
      val queryExecutionId = "query-execution-id"
      whenever(athenaClient.getQueryExecutionId(any<AthenaQuery>())).thenReturn(queryExecutionId)

      val result = repository.getPersonsQueryId(personsQueryCriteria)
      assertThat(result).isEqualTo(queryExecutionId)
    }
  }

  @Nested
  @DisplayName("GetPersonsQueryResults")
  inner class GetPersonsQueryResults {

    val simpleResultTest: String = """
      {
        "ResultSet": {
          "Rows": [
            {
              "Data": [
                {
                  "VarCharValue": "person_id"
                },
                {
                  "VarCharValue": "person_name"
                }
              ]
            },
            {
              "Data": [
                {
                  "VarCharValue": "1"
                },
                {
                  "VarCharValue": "2"
                },
              ]
            }
          ],
          "ResultSetMetadata": {
            "ColumnInfo": [
              {
                "CatalogName": "hive",
                "SchemaName": "",
                "TableName": "",
                "Name": "person_id",
                "Label": "person_id",
                "Type": "varchar",
                "Precision": 19,
                "Scale": 0,
                "Nullable": "UNKNOWN",
                "CaseSensitive": false
              },
              {
                "CatalogName": "hive",
                "SchemaName": "",
                "TableName": "",
                "Name": "person_name",
                "Label": "person_name",
                "Type": "varchar",
                "Precision": 19,
                "Scale": 0,
                "Nullable": "UNKNOWN",
                "CaseSensitive": false
              }
            ]
          }
        },
        "UpdateCount": 0
      }
    """.trimIndent()

    @Test
    fun `it should return a list of persons`() {
      val queryExecutionId = "query-execution-id"
      val expectedResult = AthenaHelper.resultSetFromJson(simpleResultTest)
      whenever(athenaClient.getQueryResult(queryExecutionId)).thenReturn(expectedResult)

      val result = repository.getPersonsQueryResults(queryExecutionId)
      assertThat(result).isNotEmpty()
      assertThat(result[0]).isInstanceOf(AthenaPersonDto::class.java)
    }
  }
}
