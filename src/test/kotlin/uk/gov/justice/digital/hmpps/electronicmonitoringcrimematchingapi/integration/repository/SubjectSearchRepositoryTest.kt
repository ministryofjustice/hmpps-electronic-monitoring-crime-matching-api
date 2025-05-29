package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.repository

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
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaSubjectInformationDTO
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject.SubjectSearchCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.SubjectSearchRepository

class SubjectSearchRepositoryTest {
  private lateinit var athenaClient: EmDatastoreClient
  private lateinit var repository: SubjectSearchRepository

  @BeforeEach
  fun setup() {
    athenaClient = mock(EmDatastoreClient::class.java)
    repository = SubjectSearchRepository(athenaClient)
  }

  @Nested
  inner class SearchSubjects {
    @Test
    fun `searchSubjects returns queryExecutionId`() {
      val subjectSearchCriteria = SubjectSearchCriteria(name = "John", nomisId = "12345")
      val queryExecutionId = "query-execution-id"
      whenever(athenaClient.getQueryExecutionId(any<AthenaQuery>())).thenReturn(queryExecutionId)

      val result = repository.searchSubjects(subjectSearchCriteria)
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
                  "VarCharValue": "nomis_id"
                }
              ]
            },
            {
              "Data": [
                {
                  "VarCharValue": "1253587"
                }
              ]
            }
          ],
          "ResultSetMetadata": {
            "ColumnInfo": [
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

      val result = repository.getSubjectSearchResults(queryExecutionId)
      assertThat(result).isNotEmpty
      assertThat(result[0]).isInstanceOf(AthenaSubjectInformationDTO::class.java)
    }
  }
}
