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
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaHelper
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Person

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
  @DisplayName("GetPersons")
  inner class GetPersons {

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
      val personsQueryCriteria = PersonsQueryCriteria(name = "name", includeDeviceActivations = true)
      val expectedResult = AthenaHelper.resultSetFromJson(simpleResultTest)

      whenever(athenaClient.getQueryResult(any<AthenaQuery>())).thenReturn(expectedResult)

      val result = repository.getPersons(personsQueryCriteria)
      assertThat(result).isNotEmpty()
      assertThat(result[0]).isInstanceOf(Person::class.java)
    }
  }
}
