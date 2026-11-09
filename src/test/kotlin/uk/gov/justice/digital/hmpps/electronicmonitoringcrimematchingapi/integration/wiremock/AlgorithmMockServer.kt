package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo

class AlgorithmMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8085
  }

  fun stubPositionSegments(
    deviceId: String,
    startTime: String,
    endTime: String,
  ) {
    stubFor(
      get(urlPathEqualTo("/devices/$deviceId/contextual-report-position-segments"))
        .withQueryParam("start_time", equalTo(startTime))
        .withQueryParam("end_time", equalTo(endTime))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "segmentationVersion": "1",
                "positionSegments": [
                  [
                    {
                      "direction": 90,
                      "gpsDate": "2026-08-17T12:00:00Z",
                      "latitude": 51.5072,
                      "longitude": -0.1276,
                      "precision": 5,
                      "speed": 0,
                      "sequenceNumber": 1
                    }
                  ]
                ]
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) """{"status":"UP"}""" else """{"status":"DOWN"}""")
          .withStatus(status),
      ),
    )
  }
}
