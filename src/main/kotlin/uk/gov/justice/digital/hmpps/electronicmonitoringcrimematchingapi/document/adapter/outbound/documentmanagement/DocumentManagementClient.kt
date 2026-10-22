package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.documentmanagement

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.server.ResponseStatusException
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import java.time.Duration
import java.util.*

@Component
class DocumentManagementClient(
  private val documentManagementApiWebClient: WebClient,
) {
  companion object {
    const val SERVICE_NAME_HEADER = "hmpps-electronic-monitoring-crime-matching-api"
    private val logger = LoggerFactory.getLogger(this::class.java)
  }

  fun upload(
    documentId: UUID,
    document: GeneratedDocument,
  ): DocumentUploadResponse {
    val multipart = MultipartBodyBuilder().apply {
      part("file", document.content)
        .contentType(MediaType.parseMediaType(document.contentType))
        .filename(document.filename)

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
          }
          .onRetryExhaustedThrow { _, signal -> signal.failure() },
      )
      .block()
      ?: throw IllegalStateException("Document management API returned an empty response")
  }

  fun get(documentUuid: UUID): ByteArray? = try {
    documentManagementApiWebClient
      .get()
      .uri("/documents/$documentUuid/file")
      .header("Service-Name", SERVICE_NAME_HEADER)
      .retrieve()
      .bodyToMono<ByteArray>()
      .retryWhen(
        Retry.fixedDelay(2, Duration.ofMillis(2000))
          .filter {
            it is WebClientResponseException &&
              (
                it.statusCode.is5xxServerError ||
                  it.statusCode.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS) ||
                  it.statusCode.isSameCodeAs(HttpStatus.REQUEST_TIMEOUT)
                )
          }
          .onRetryExhaustedThrow { _, signal -> signal.failure() },
      )
      .block()
  } catch (e: WebClientResponseException) {
    logger.error(
      "Document Management API gave {} status code for document {}. Response body: {}",
      e.statusCode.value(),
      documentUuid,
      e.responseBodyAsString,
      e,
    )
    throw ResponseStatusException(
      e.statusCode,
      "Document Management API gave ${e.statusCode.value()} status code",
      e,
    )
  } catch (e: Exception) {
    logger.error("Error getting document artefact from Document Management API: $documentUuid", e)
    throw ResponseStatusException(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "Document Management API gave ${HttpStatus.INTERNAL_SERVER_ERROR.value()} status code",
      e,
    )
  }
}
