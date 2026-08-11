package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

class NotifyMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8092
  }

  fun stubSendEmail() {
    stubFor(
      post(urlEqualTo("/v2/notifications/email"))
        .willReturn(
          aResponse()
            .withStatus(201)
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "id": "00000000-0000-0000-0000-000000000001",
                "reference": "test-reference",
                "content": {
                  "body": "Test email body",
                  "from_email": "test@example.com",
                  "subject": "Test subject"
                },
                "template": {
                  "id": "00000000-0000-0000-0000-000000000002",
                  "version": 1,
                  "uri": "https://api.notifications.service.gov.uk/v2/template/test"
                },
                "one_click_unsubscribe_url": null,
                "sanitised_content": null
              }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun verifyEmailSentTo(address: String, count: Int) = verify(
    count,
    postRequestedFor(urlEqualTo("/v2/notifications/email"))
      .withRequestBody(matchingJsonPath("$.email_address", equalTo(address))),
  )
}
