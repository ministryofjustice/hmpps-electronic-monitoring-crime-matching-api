package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.documentmanagement

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import java.time.Duration
import java.util.UUID

@Component
class DocumentManagementClient(
  private val documentManagementApiWebClient: WebClient,
) {
  companion object {
    const val SERVICE_NAME_HEADER = "hmpps-electronic-monitoring-crime-matching-api"
  }

  fun upload(
    documentId: UUID,
    document: GeneratedDocument,
  ): DocumentUploadResponse {
    val multipart = MultipartBodyBuilder().apply {
      part("file", document.content)
        .contentType(MediaType.parseMediaType(document.contentType))
        .filename(document.filename)

      // TODO Agree on what metadata to send, for now sending empty as it's required
      // Possibly the documentType and the original document request ID?
      part("metadata", "{}")
        .contentType(MediaType.APPLICATION_JSON)
    }

    return documentManagementApiWebClient.post()
      .uri("/documents/EMAC_REPORT/{id}", documentId)
      .header("Service-Name", SERVICE_NAME_HEADER)
      .body(BodyInserters.fromMultipartData(multipart.build()))
      .retrieve()
      .bodyToMono<DocumentUploadResponse>()
      .retryWhen(
        Retry.fixedDelay(2, Duration.ofMillis(2000))
          // Only retry on 5xx/429/408 status error, do not retry for bad request
          .filter {
            it is WebClientResponseException &&
              (
                it.statusCode.is5xxServerError ||
                  it.statusCode.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS) ||
                  it.statusCode.isSameCodeAs(HttpStatus.REQUEST_TIMEOUT)
                )
          },
      )
      .block()
      ?: throw IllegalStateException("Document management API returned an empty response")
  }
}
