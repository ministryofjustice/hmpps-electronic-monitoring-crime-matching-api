package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.adapter.outbound.positionsegments

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.util.retry.Retry
import java.time.Duration

@Component
class PositionSegmentsClient(
  private val algorithmApiWebClient: WebClient,
) {
  fun get(deviceActivationId: String, startTime: String, endTime: String): PositionSegmentsResponse = algorithmApiWebClient
    .get()
    .uri {
      it.path("/device-activations/$deviceActivationId/contextual-report-position-segments")
        .queryParam("start_time", startTime)
        .queryParam("end_time", endTime)
        .build()
    }
    .retrieve()
    .bodyToMono<PositionSegmentsResponse>()
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
    ?: throw IllegalStateException("Algorithm API returned an empty response")
}
