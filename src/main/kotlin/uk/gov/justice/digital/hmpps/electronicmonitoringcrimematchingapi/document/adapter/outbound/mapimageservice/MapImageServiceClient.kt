package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.util.retry.Retry
import java.time.Duration

@Component
class MapImageServiceClient(
  private val mapImageServiceApiWebClient: WebClient,
) {
  fun render(request: MapImageServiceRequest): ByteArray = mapImageServiceApiWebClient
    .post()
    .uri("/map-image")
    .contentType(MediaType.APPLICATION_JSON)
    .accept(MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG)
    .bodyValue(request)
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
    ?: throw IllegalStateException("Map Image Service returned an empty response")
}
