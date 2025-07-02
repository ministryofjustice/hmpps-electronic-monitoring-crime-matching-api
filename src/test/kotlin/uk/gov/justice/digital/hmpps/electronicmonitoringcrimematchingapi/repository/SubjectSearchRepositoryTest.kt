package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client.EmDatastoreClient
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaHelper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaSubjectDTO
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.subject.SubjectRepository

class SubjectSearchRepositoryTest {
  private lateinit var athenaClient: EmDatastoreClient
  private lateinit var repository: SubjectRepository

  @BeforeEach
  fun setup() {
    athenaClient = mock(EmDatastoreClient::class.java)
    repository = SubjectRepository(athenaClient)
  }

  @Nested
  inner class SearchSubjects {
    @Test
    fun `searchSubjects returns queryExecutionId`() {
      val subjectsQueryCriteria = SubjectsQueryCriteria(name = "John", nomisId = "12345")
      val queryExecutionId = "query-execution-id"
      whenever(athenaClient.getQueryExecutionId(any<AthenaQuery>())).thenReturn(queryExecutionId)

      val result = repository.getSubjectsQueryId(subjectsQueryCriteria)
      assertThat(result).isEqualTo(queryExecutionId)
    }
  }

  @Nested
  @DisplayName("GetSubjectSearchResults")
  inner class GetSubjectSearchResults {

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
                  "VarCharValue": "nomis_id"
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
                "Name": "nomis_id",
                "Label": "nomis_id",
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
    fun `it should return list of subject results`() {
      val queryExecutionId = "query-execution-id"
      val expectedResult = AthenaHelper.resultSetFromJson(simpleResultTest)
      whenever(athenaClient.getQueryResult(queryExecutionId)).thenReturn(expectedResult)

      val result = repository.getSubjectsQueryResults(queryExecutionId)
      assertThat(result).isNotEmpty
      assertThat(result[0]).isInstanceOf(AthenaSubjectDTO::class.java)
    }
  }
}
