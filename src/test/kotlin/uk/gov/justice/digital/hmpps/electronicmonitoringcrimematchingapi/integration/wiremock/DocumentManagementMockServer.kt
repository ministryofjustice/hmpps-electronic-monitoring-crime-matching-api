package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aMultipart
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.documentmanagement.DocumentManagementClient.Companion.SERVICE_NAME_HEADER
import java.util.UUID

class DocumentManagementMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8094
  }

  fun stubUploadDocument() {
    stubFor(
      post(urlPathTemplate("/documents/EMAC_REPORT/{documentId}"))
        .withMultipartRequestBody(aMultipart().withName("file"))
        .withMultipartRequestBody(aMultipart().withName("metadata"))
        .withHeader("Service-Name", equalTo(SERVICE_NAME_HEADER))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "documentUuid": "{{request.path.documentId}}",
                "documentFilename": "placeholder",
                "mimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
              }
              """.trimIndent(),
            )
            .withTransformers("response-template")
            .withStatus(201),
        ),
    )
  }

  fun stubGetDocumentFile(
    documentUuid: UUID? = null,
    status: Int = 200,
    responseBody: String = "document artefact content",
    contentType: String = "application/pdf",
  ) {
    val matcher = if (documentUuid == null) {
      urlPathMatching("/documents/[^/]+/file")
    } else {
      urlPathEqualTo("/documents/$documentUuid/file")
    }

    stubFor(
      get(matcher)
        .withHeader("Service-Name", equalTo(SERVICE_NAME_HEADER))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", contentType)
            .withStatus(status)
            .withBody(responseBody),
        ),
    )
  }

  fun stubUploadDocumentFailure(status: Int) {
    stubFor(
      post(urlPathMatching("/documents/EMAC_REPORT/[^/]+"))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withHeader("Content-Type", "application/json")
            .withBody("""{"message": "Document upload failed"}"""),
        ),
    )
  }

  fun verifyUploadRequestCount(count: Int) {
    verify(count, postRequestedFor(urlPathMatching("/documents/EMAC_REPORT/[^/]+")))
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
