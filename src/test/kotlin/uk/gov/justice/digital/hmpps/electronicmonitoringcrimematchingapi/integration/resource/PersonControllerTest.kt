package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.DeviceActivationResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PagedResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.Response
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal.S3AsyncService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.io.StringWriter

@ActiveProfiles("integration")
class PersonControllerTest : IntegrationTestBase() {

  @MockitoBean
  lateinit var s3AsyncService: S3AsyncService

  @Nested
  @DisplayName("GET /persons")
  inner class GetPersons {
    @Test
    fun `it should return persons with device activations`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/persons.device-activations.success.json",
      )

      val sqlExpressions = stubPagedPersonS3Select(
        listOf(
          personCsvRow(),
        ),
      )

      val result = webTestClient.get()
        .uri("/persons?name=name&page=0&pageSize=1")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CASELOAD__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PagedResponse<PersonResponse>>()
        .returnResult()
        .responseBody!!

      assertThat(result.data).isNotNull()
      assertThat(result.data).hasSize(1)
      assertThat(result.data[0].deviceActivations).hasSize(1)
      assertThat(result.data[0]).isEqualTo(
        PersonResponse(
          personId = "1",
          name = "first_name last_name",
          nomisId = "nomis_id",
          pncRef = "pnc_id",
          dateOfBirth = "2000-05-29",
          probationPractitioner = "responsible_officer_name",
          address = "street, city, zip",
          deviceActivations = listOf(
            DeviceActivationResponse(
              deviceActivationId = 54321,
              deviceId = 12345,
              deviceName = "",
              deviceSerialNumber = "987654321",
              personId = "1",
              deviceActivationDate = "2023-05-18T00:00",
              deviceDeactivationDate = "2024-05-18T00:00",
              orderStart = "",
              orderEnd = "",
            ),
          ),
        ),
      )
      assertThat(result.pageCount).isEqualTo(1)
      assertThat(result.pageSize).isEqualTo(1)
      assertThat(result.pageNumber).isEqualTo(0)
      assertThat(sqlExpressions).containsExactly(
        "SELECT * FROM s3object WHERE CAST(row_number AS INT) > 0 LIMIT 1",
        "SELECT COUNT(*) FROM s3object",
      )
    }

    @Test
    fun `it should return the second page of persons with device activations`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/persons.device-activations.success.json",
      )

      val sqlExpressions = stubPagedPersonS3Select(
        listOf(
          personCsvRow(),
          personCsvRow(personId = "2", rowNumber = 2),
        ),
        offset = 1,
        limit = 1,
      )

      val result = webTestClient.get()
        .uri("/persons?name=name&page=1&pageSize=1")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CASELOAD__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PagedResponse<PersonResponse>>()
        .returnResult()
        .responseBody!!

      assertThat(result.data).isNotNull()
      assertThat(result.data).hasSize(1)
      assertThat(result.data[0].deviceActivations).hasSize(1)
      assertThat(result.data[0]).isEqualTo(
        PersonResponse(
          personId = "2",
          name = "first_name last_name",
          nomisId = "nomis_id",
          pncRef = "pnc_id",
          dateOfBirth = "2000-05-29",
          probationPractitioner = "responsible_officer_name",
          address = "street, city, zip",
          deviceActivations = listOf(
            DeviceActivationResponse(
              deviceActivationId = 54321,
              deviceId = 12345,
              deviceName = "",
              deviceSerialNumber = "987654321",
              personId = "2",
              deviceActivationDate = "2023-05-18T00:00",
              deviceDeactivationDate = "2024-05-18T00:00",
              orderStart = "",
              orderEnd = "",
            ),
          ),
        ),
      )
      assertThat(result.pageCount).isEqualTo(2)
      assertThat(result.pageSize).isEqualTo(1)
      assertThat(result.pageNumber).isEqualTo(1)
      assertThat(sqlExpressions).containsExactly(
        "SELECT * FROM s3object WHERE CAST(row_number AS INT) > 1 LIMIT 1",
        "SELECT COUNT(*) FROM s3object",
      )
    }

    @Test
    fun `it should return persons with a field containing a comma`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/persons.device-activations.success.json",
      )

      stubPagedPersonS3Select(
        listOf(
          personCsvRow(street = "next,street"),
        ),
      )

      val result = webTestClient.get()
        .uri("/persons?name=name")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CASELOAD__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PagedResponse<PersonResponse>>()
        .returnResult()
        .responseBody!!

      assertThat(result.data).isNotNull()
      assertThat(result.data).hasSize(1)
      assertThat(result.data[0].deviceActivations).hasSize(1)
      assertThat(result.data[0]).isEqualTo(
        PersonResponse(
          personId = "1",
          name = "first_name last_name",
          nomisId = "nomis_id",
          pncRef = "pnc_id",
          dateOfBirth = "2000-05-29",
          probationPractitioner = "responsible_officer_name",
          address = "next,street, city, zip",
          deviceActivations = listOf(
            DeviceActivationResponse(
              deviceActivationId = 54321,
              deviceId = 12345,
              deviceName = "",
              deviceSerialNumber = "987654321",
              personId = "1",
              deviceActivationDate = "2023-05-18T00:00",
              deviceDeactivationDate = "2024-05-18T00:00",
              orderStart = "",
              orderEnd = "",
            ),
          ),
        ),
      )
    }

    @Test
    fun `it should return an empty result when no persons exist`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/persons.device-activations.empty.success.json",
      )

      val sqlExpressions = stubPagedPersonS3Select(
        personRows = emptyList(),
      )

      val result = webTestClient.get()
        .uri("/persons?name=name&page=0&pageSize=1")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CASELOAD__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PagedResponse<PersonResponse>>()
        .returnResult()
        .responseBody!!

      assertThat(result.data).isNotNull()
      assertThat(result.data).hasSize(0)
      assertThat(result.pageCount).isEqualTo(0)
      assertThat(result.pageSize).isEqualTo(1)
      assertThat(result.pageNumber).isEqualTo(0)
      assertThat(sqlExpressions).containsExactly(
        "SELECT * FROM s3object WHERE CAST(row_number AS INT) > 0 LIMIT 1",
        "SELECT COUNT(*) FROM s3object",
      )
    }

    @Test
    fun `it should return persons with a null device deactivation date if the device activation has the sentinel date value`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/persons.device-activations-sentinel-date-value.success.json",
      )

      stubPagedPersonS3Select(
        listOf(
          personCsvRow(deactivationDate = "9999-12-31 00:00:00.000"),
        ),
      )

      val result = webTestClient.get()
        .uri("/persons?name=name")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CASELOAD__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PagedResponse<PersonResponse>>()
        .returnResult()
        .responseBody!!

      assertThat(result.data).isNotNull()
      assertThat(result.data).hasSize(1)
      assertThat(result.data[0].deviceActivations).hasSize(1)
      assertThat(result.data[0]).isEqualTo(
        PersonResponse(
          personId = "1",
          name = "first_name last_name",
          nomisId = "nomis_id",
          pncRef = "pnc_id",
          dateOfBirth = "2000-05-29",
          probationPractitioner = "responsible_officer_name",
          address = "street, city, zip",
          deviceActivations = listOf(
            DeviceActivationResponse(
              deviceActivationId = 54321,
              deviceId = 12345,
              deviceName = "",
              deviceSerialNumber = "987654321",
              personId = "1",
              deviceActivationDate = "2023-05-18T00:00",
              deviceDeactivationDate = null,
              orderStart = "",
              orderEnd = "",
            ),
          ),
        ),
      )
    }

    @Test
    fun `it should fail with bad request when invalid criteria fields are passed`() {
      webTestClient.get()
        .uri("/persons")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CASELOAD__RO")))
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `it should return an INTERNAL_SERVER_ERROR response if the Athena query fails`() {
      stubFailedQueryExecution("123")

      val response = webTestClient.get()
        .uri("/persons?name=name")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CASELOAD__RO")))
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody(ErrorResponse::class.java)
        .returnResult()
        .responseBody!!

      assertThat(response).isEqualTo(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: There was an unexpected error processing the request.",
          developerMessage = "There was an unexpected error processing the request.",
        ),
      )
    }
  }

  @Nested
  @DisplayName("GET /persons/{personId}")
  inner class GetPerson {
    @Test
    fun `it should return a NOT_FOUND response if person was not found in Athena`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/person.empty.success.json",
      )

      val response = webTestClient.get()
        .uri("/persons/1")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CASELOAD__RO")))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult()
        .responseBody!!

      assertThat(response).isEqualTo(
        ErrorResponse(
          status = 404,
          userMessage = "Not Found",
          developerMessage = "No person found with id: 1",
        ),
      )
    }

    @Test
    fun `it should return an OK response if person was found in Athena`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/persons.some.success.json",
      )

      val result = webTestClient.get()
        .uri("/persons/1")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CASELOAD__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<Response<PersonResponse>>()
        .returnResult()
        .responseBody!!

      assertThat(result.data).isEqualTo(
        PersonResponse(
          personId = "1",
          name = "first_name last_name",
          nomisId = "nomis_id",
          pncRef = "pnc_id",
          dateOfBirth = "2000-05-29",
          probationPractitioner = "responsible_officer_name",
          address = "street, city, zip",
          deviceActivations = listOf(),
        ),
      )
    }

    @Test
    fun `it should use the cached query execution when a duplicate request is made`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/persons.some.success.json",
      )

      webTestClient.get()
        .uri("/persons/1")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CASELOAD__RO")))
        .exchange()
        .expectStatus()
        .isOk

      webTestClient.get()
        .uri("/persons/1")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CASELOAD__RO")))
        .exchange()
        .expectStatus()
        .isOk

      // Only one query should have been started
      verifyAthenaStartQueryExecutionCount(1)
      // The status of the existing query should have been checked twice
      verifyAthenaGetQueryExecutionCount(2)
      // The results of the existing query should have been used twice
      verifyAthenaGetQueryResultsCount(2)
    }

    @Test
    fun `it should return an INTERNAL_SERVER_ERROR response if the Athena query fails`() {
      stubFailedQueryExecution("123")

      val response = webTestClient.get()
        .uri("/persons/1")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CASELOAD__RO")))
        .exchange()
        .expectStatus()
        .is5xxServerError
        .expectBody(ErrorResponse::class.java)
        .returnResult()
        .responseBody!!

      assertThat(response).isEqualTo(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: There was an unexpected error processing the request.",
          developerMessage = "There was an unexpected error processing the request.",
        ),
      )
    }

    @Test
    fun `it should keep retrying to get query results until the query is finished`() {
      stubQueryExecution(
        "123",
        3,
        "SUCCEEDED",
        "athenaResponses/persons.some.success.json",
      )

      webTestClient.get()
        .uri("/persons/1")
        .headers(setAuthorisation(roles = listOf("ROLE_EM_CRIME_MATCHING__CASELOAD__RO")))
        .exchange()
        .expectStatus()
        .isOk

      // Only one query should have been started
      verifyAthenaStartQueryExecutionCount(1)
      // The status of the existing query should have been checked twice
      verifyAthenaGetQueryExecutionCount(3)
      // The results of the existing query should have been used twice
      verifyAthenaGetQueryResultsCount(1)
    }
  }

  private fun stubPagedPersonS3Select(
    personRows: List<List<String>>,
    totalRecords: Int = personRows.size,
    offset: Int = 0,
    limit: Int = 1,
  ): List<String> {
    val sqlExpressions = mutableListOf<String>()

    whenever(s3AsyncService.selectObjectContent(any(), any(), any())).thenAnswer { invocation ->
      val sqlExpression = invocation.getArgument<String>(2)
      sqlExpressions += sqlExpression

      if (sqlExpression.contains("COUNT(*)")) {
        "$totalRecords\n"
      } else {
        personRows
          .filter { it.last().toInt() > offset }
          .take(limit)
          .joinToString(separator = "") { rowAsCsv(it) }
      }
    }

    return sqlExpressions
  }

  private fun personCsvRow(
    personId: String = "1",
    firstName: String = "first_name",
    lastName: String = "last_name",
    street: String = "street",
    deactivationDate: String = "2024-05-18 00:00:00.000",
    rowNumber: Int = 1,
  ): List<String> = listOf(
    personId,
    firstName,
    lastName,
    "nomis_id",
    "pnc_id",
    "2000-05-29",
    "responsible_officer_name",
    "zip",
    "city",
    street,
    "12345",
    "54321",
    "987654321",
    "2023-05-18 00:00:00.000",
    deactivationDate,
    rowNumber.toString(),
  )

  private fun rowAsCsv(row: List<String>): String = StringWriter().use { writer ->
    CSVPrinter(writer, CSVFormat.DEFAULT).use { printer ->
      printer.printRecord(row)
    }
    writer.toString()
  }
}
