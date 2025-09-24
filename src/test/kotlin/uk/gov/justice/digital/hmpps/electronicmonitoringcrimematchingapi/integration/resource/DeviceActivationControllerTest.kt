package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.DeviceActivationDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PositionDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.ResponseDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.IntegrationTestBase
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@ActiveProfiles("integration")
class DeviceActivationControllerTest : IntegrationTestBase() {
  @Nested
  @DisplayName("GET /device-activations/{deviceActivationId}")
  inner class GetDeviceActivation {
    @Test
    fun `it should return a 401 if the request is not authenticated`() {
      webTestClient.get()
        .uri("/device-activations/1")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return a BAD_REQUEST if param is not a number`() {
      val result = webTestClient.get()
        .uri("/device-activations/abc")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ErrorResponse>()
        .returnResult()
        .responseBody!!

      assertThat(result).isEqualTo(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "The provided value 'abc' is the incorrect type for the 'deviceActivationId' parameter.",
          developerMessage = "The provided value 'abc' is the incorrect type for the 'deviceActivationId' parameter.",
        ),
      )
    }

    @Test
    fun `it should return a NOT_FOUND response if device activation was not found in Athena`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/device-activation.empty.success.json",
      )

      webTestClient.get()
        .uri("/device-activations/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isNotFound
    }

    @Test
    fun `it should return an OK response if device activation was found in Athena`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/device-activation.inactive.success.json",
      )

      val result = webTestClient.get()
        .uri("/device-activations/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ResponseDto<DeviceActivationDto>>()
        .returnResult()
        .responseBody!!

      assertThat(result.data).isEqualTo(
        DeviceActivationDto(
          deviceActivationId = 12345,
          deviceId = 54321,
          deviceName = "",
          personId = 98765,
          deviceActivationDate = "2023-05-18T00:00",
          deviceDeactivationDate = "2023-05-18T00:00",
          orderStart = "",
          orderEnd = "",
        ),
      )
    }

    @Test
    fun `it should return a null deactivation date if the device activation is active`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/device-activation.active.success.json",
      )

      val result = webTestClient.get()
        .uri("/device-activations/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ResponseDto<DeviceActivationDto>>()
        .returnResult()
        .responseBody!!

      assertThat(result.data).isEqualTo(
        DeviceActivationDto(
          deviceActivationId = 12345,
          deviceId = 54321,
          deviceName = "",
          personId = 98765,
          deviceActivationDate = "2023-05-18T00:00",
          deviceDeactivationDate = null,
          orderStart = "",
          orderEnd = "",
        ),
      )
    }

    @Test
    fun `it should use the cached query execution when a duplicate request is made`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/device-activation.inactive.success.json",
      )

      webTestClient.get()
        .uri("/device-activations/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk

      webTestClient.get()
        .uri("/device-activations/1")
        .headers(setAuthorisation())
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
        .uri("/device-activations/1")
        .headers(setAuthorisation())
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
        "athenaResponses/device-activation.inactive.success.json",
      )

      webTestClient.get()
        .uri("/device-activations/1")
        .headers(setAuthorisation())
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

  @Nested
  @DisplayName("GET /device-activations/{deviceActivationId}/positions")
  inner class GetDeviceActivationPositions {
    @Test
    fun `it should return a 401 if the request is not authenticated`() {
      webTestClient.get()
        .uri("/device-activations/1/positions")
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `it should return a BAD_REQUEST if param is not a number`() {
      val result = webTestClient.get()
        .uri("/device-activations/abc/positions")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ErrorResponse>()
        .returnResult()
        .responseBody!!

      assertThat(result).isEqualTo(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "The provided value 'abc' is the incorrect type for the 'deviceActivationId' parameter.",
          developerMessage = "The provided value 'abc' is the incorrect type for the 'deviceActivationId' parameter.",
        ),
      )
    }

    @Test
    fun `it should return an OK response with an empty list if no positions found in Athena`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/device-activation.positions.empty.success.json",
      )

      val result = webTestClient.get()
        .uri("/device-activations/1/positions")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ResponseDto<List<PositionDto>>>()
        .returnResult()
        .responseBody!!

      assertThat(result.data).isEmpty()
    }

    @Test
    fun `it should return an OK response with positions if positions found in Athena`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/device-activation.positions.some.success.json",
      )

      val result = webTestClient.get()
        .uri("/device-activations/1/positions")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ResponseDto<List<PositionDto>>>()
        .returnResult()
        .responseBody!!

      assertThat(result.data).isEqualTo(
        listOf(
          PositionDto(
            positionId = 1,
            latitude = 51.574865,
            longitude = 0.060977,
            precision = 100,
            speed = 1,
            direction = 52,
            timestamp = "2025-09-10T07:51:08",
            geolocationMechanism = "GPS",
          ),
          PositionDto(
            positionId = 2,
            latitude = 51.574153,
            longitude = 0.058536,
            precision = 400,
            speed = 10,
            direction = 27,
            timestamp = "2025-09-08T17:30:07",
            geolocationMechanism = "RF",
          ),
          PositionDto(
            positionId = 3,
            latitude = 51.573248244162706,
            longitude = 0.05111371418603764,
            precision = 400,
            speed = 10,
            direction = 27,
            timestamp = "2025-09-08T17:30:08",
            geolocationMechanism = "LBS",
          ),
          PositionDto(
            positionId = 4,
            latitude = 51.574622,
            longitude = 0.048643,
            precision = 400,
            speed = 10,
            direction = 27,
            timestamp = "2025-09-08T17:30:09",
            geolocationMechanism = "WIFI",
          ),
        ),
      )
    }

    @Test
    fun `it should return a BAD_REQUEST response if geolocation mechanism is not valid`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/device-activation.positions.some.success.json",
      )

      val result = webTestClient.get()
        .uri("/device-activations/1/positions?geolocationMechanism=abc")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ErrorResponse>()
        .returnResult()
        .responseBody!!

      assertThat(result).isEqualTo(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "The provided value 'abc' is the incorrect type for the 'geolocationMechanism' parameter.",
          developerMessage = "The provided value 'abc' is the incorrect type for the 'geolocationMechanism' parameter.",
        ),
      )
    }

    @Test
    fun `it should return a BAD_REQUEST response if from date is not valid`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/device-activation.positions.some.success.json",
      )

      val result = webTestClient.get()
        .uri("/device-activations/1/positions?from=abc")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ErrorResponse>()
        .returnResult()
        .responseBody!!

      assertThat(result).isEqualTo(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "The provided value 'abc' is the incorrect type for the 'from' parameter.",
          developerMessage = "The provided value 'abc' is the incorrect type for the 'from' parameter.",
        ),
      )
    }

    @Test
    fun `it should return a BAD_REQUEST response if to date is not valid`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/device-activation.positions.some.success.json",
      )

      val result = webTestClient.get()
        .uri("/device-activations/1/positions?to=abc")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody<ErrorResponse>()
        .returnResult()
        .responseBody!!

      assertThat(result).isEqualTo(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "The provided value 'abc' is the incorrect type for the 'to' parameter.",
          developerMessage = "The provided value 'abc' is the incorrect type for the 'to' parameter.",
        ),
      )
    }

    @Test
    fun `it should filter devices by geolocation mechanism in Athena query`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/device-activation.positions.some.success.json",
      )

      webTestClient.get()
        .uri("/device-activations/1/positions?geolocationMechanism=GPS")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ResponseDto<List<PositionDto>>>()
        .returnResult()
        .responseBody!!

      // Check that the WHERE clause include the geolocation mechanism filter
      verifyAthenaStartQueryExecutionWithQuery(
        "WHERE d.device_activation_id = ? AND p.position_lbs = ?",
        listOf("1", "1"),
      )
    }

    @Test
    fun `it should filter devices by from date in Athena query`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/device-activation.positions.some.success.json",
      )

      webTestClient.get()
        .uri("/device-activations/1/positions?from=2025-01-01T00:00:00Z")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ResponseDto<List<PositionDto>>>()
        .returnResult()
        .responseBody!!

      // Check that the WHERE clause include the geolocation mechanism filter
      verifyAthenaStartQueryExecutionWithQuery(
        "WHERE d.device_activation_id = ? AND p.position_gps_date >= from_iso8601_timestamp(?)",
        listOf("1", "'2025-01-01T00:00Z'"),
      )
    }

    @Test
    fun `it should filter devices by to date in Athena query`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/device-activation.positions.some.success.json",
      )

      webTestClient.get()
        .uri("/device-activations/1/positions?to=2025-01-01T00:00:00Z")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ResponseDto<List<PositionDto>>>()
        .returnResult()
        .responseBody!!

      // Check that the WHERE clause include the geolocation mechanism filter
      verifyAthenaStartQueryExecutionWithQuery(
        "WHERE d.device_activation_id = ? AND p.position_gps_date <= from_iso8601_timestamp(?)",
        listOf("1", "'2025-01-01T00:00Z'"),
      )
    }

    @Test
    fun `it should use the cached query execution when a duplicate request is made`() {
      stubQueryExecution(
        "123",
        1,
        "SUCCEEDED",
        "athenaResponses/device-activation.positions.some.success.json",
      )

      webTestClient.get()
        .uri("/device-activations/1/positions")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk

      webTestClient.get()
        .uri("/device-activations/1/positions")
        .headers(setAuthorisation())
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
        .uri("/device-activations/1/positions")
        .headers(setAuthorisation())
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
        "athenaResponses/device-activation.positions.some.success.json",
      )

      webTestClient.get()
        .uri("/device-activations/1/positions")
        .headers(setAuthorisation())
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
}
