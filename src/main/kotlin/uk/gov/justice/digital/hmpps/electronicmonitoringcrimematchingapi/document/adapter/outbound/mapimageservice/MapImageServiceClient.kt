package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

@Component
class MapImageServiceClient(
  private val mapImageServiceApiWebClient: WebClient,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val DEFAULT_RETRY_DELAY = Duration.ofSeconds(2)
  }

  fun render(request: MapImageServiceRequest): ByteArray = mapImageServiceApiWebClient
    .post()
    .uri("/map-image")
    .contentType(MediaType.APPLICATION_JSON)
    .accept(MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG)
    .bodyValue(request)
    .retrieve()
    .bodyToMono<ByteArray>()
    .retryWhen(
      Retry.max(2)
        .filter {
          it is WebClientResponseException &&
            (
              it.statusCode.is5xxServerError ||
                it.statusCode.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS) ||
                it.statusCode.isSameCodeAs(HttpStatus.REQUEST_TIMEOUT)
              )
        }
        .doBeforeRetryAsync { signal -> Mono.delay(retryDelay(signal.failure())).then() }
        .onRetryExhaustedThrow { _, signal -> signal.failure() },
    )
    .block()
    ?: throw IllegalStateException("Map Image Service returned an empty response")

  private fun retryDelay(failure: Throwable): Duration {
    if (failure !is WebClientResponseException || !failure.statusCode.isSameCodeAs(HttpStatus.SERVICE_UNAVAILABLE)) {
      return DEFAULT_RETRY_DELAY
    }

    val retryAfterSeconds = failure.headers.getFirst(HttpHeaders.RETRY_AFTER)?.trim()?.toLongOrNull()
    if (retryAfterSeconds != null && retryAfterSeconds >= 0) {
      return Duration.ofSeconds(retryAfterSeconds)
    }

    logger.warn(
      "Map Image Service returned 503 with a missing or invalid Retry-After header; retrying after {} seconds",
      DEFAULT_RETRY_DELAY.seconds,
    )
    return DEFAULT_RETRY_DELAY
  }
}
