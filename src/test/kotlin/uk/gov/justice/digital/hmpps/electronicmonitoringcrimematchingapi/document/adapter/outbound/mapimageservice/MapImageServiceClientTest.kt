package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.test.scheduler.VirtualTimeScheduler
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MapImageServiceClientTest {
  private val executor = Executors.newSingleThreadExecutor()
  private val attempts = AtomicInteger()
  private lateinit var scheduler: VirtualTimeScheduler
  private val request = MapImageServiceRequest(
    viewport = MapImageServiceRequest.Viewport(),
    layers = listOf(
      MapImageServiceRequest.LocationsLayer(
        id = "test-locations",
        points = listOf(MapImageServiceRequest.Coordinate(latitude = 51.5, longitude = -0.1)),
      ),
    ),
  )

  @BeforeEach
  fun setUp() {
    scheduler = VirtualTimeScheduler.getOrSet()
  }

  @AfterEach
  fun tearDown() {
    executor.shutdownNow()
    assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue()
    VirtualTimeScheduler.reset()
  }

  @Test
  fun `it should return the image without retrying a successful request`() {
    val result = render(client { success() })

    assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("map image".toByteArray())
    assertThat(attempts.get()).isEqualTo(1)
  }

  @ParameterizedTest
  @ValueSource(longs = [1, 5])
  fun `it should wait exactly Retry-After seconds before retrying a 503`(seconds: Long) {
    val result = render(
      client { attempt ->
        if (attempt == 1) failure(503, seconds.toString()) else success()
      },
    )

    advanceToRetry(seconds, expectedAttemptsBeforeRetry = 1)

    assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("map image".toByteArray())
    assertThat(attempts.get()).isEqualTo(2)
  }

  @Test
  fun `it should use the Retry-After header from each 503 response`() {
    val result = render(
      client { attempt ->
        when (attempt) {
          1 -> failure(503, "1")
          2 -> failure(503, "4")
          else -> success()
        }
      },
    )

    advanceToRetry(1, expectedAttemptsBeforeRetry = 1)
    advanceToRetry(4, expectedAttemptsBeforeRetry = 2)

    assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("map image".toByteArray())
    assertThat(attempts.get()).isEqualTo(3)
  }

  @Test
  fun `it should retry immediately when Retry-After is zero`() {
    val result = render(
      client { attempt ->
        if (attempt == 1) failure(503, "0") else success()
      },
    )

    assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("map image".toByteArray())
    assertThat(attempts.get()).isEqualTo(2)
    assertThat(scheduler.now(TimeUnit.SECONDS)).isZero()
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = ["invalid", "-1", "1.5", "999999999999999999999999"])
  fun `it should keep the two second delay for a missing or invalid Retry-After header`(retryAfter: String?) {
    val result = render(
      client { attempt ->
        if (attempt == 1) failure(503, retryAfter) else success()
      },
    )

    advanceToRetry(2, expectedAttemptsBeforeRetry = 1)

    assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("map image".toByteArray())
    assertThat(attempts.get()).isEqualTo(2)
  }

  @ParameterizedTest
  @ValueSource(ints = [500, 502, 504, 429, 408])
  fun `it should keep the two second delay for other retryable statuses`(status: Int) {
    val result = render(
      client { attempt ->
        if (attempt == 1) failure(status, "5") else success()
      },
    )

    advanceToRetry(2, expectedAttemptsBeforeRetry = 1)

    assertThat(result.get(5, TimeUnit.SECONDS)).isEqualTo("map image".toByteArray())
    assertThat(attempts.get()).isEqualTo(2)
  }

  @Test
  fun `it should propagate the last 503 response after two retries`() {
    val result = render(client { attempt -> failure(503, attempt.toString()) })

    advanceToRetry(1, expectedAttemptsBeforeRetry = 1)
    advanceToRetry(2, expectedAttemptsBeforeRetry = 2)

    val failure = assertThrows<ExecutionException> { result.get(5, TimeUnit.SECONDS) }.cause
    assertThat(failure).isInstanceOf(WebClientResponseException.ServiceUnavailable::class.java)
    assertThat((failure as WebClientResponseException).headers.getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("3")
    assertThat(failure.responseBodyAsString).isEqualTo("""{"code":"RENDER_BUSY"}""")
    assertThat(attempts.get()).isEqualTo(3)
  }

  @ParameterizedTest
  @ValueSource(ints = [400, 401, 403, 404])
  fun `it should not retry non-retryable statuses`(status: Int) {
    val result = render(client { failure(status, "1") })

    val failure = assertThrows<ExecutionException> { result.get(5, TimeUnit.SECONDS) }.cause
    assertThat(failure).isInstanceOf(WebClientResponseException::class.java)
    assertThat((failure as WebClientResponseException).statusCode.value()).isEqualTo(status)
    assertThat(attempts.get()).isEqualTo(1)
  }

  @Test
  fun `it should reject an empty image response without retrying`() {
    val result = render(client { ClientResponse.create(HttpStatus.OK).build() })

    val failure = assertThrows<ExecutionException> { result.get(5, TimeUnit.SECONDS) }.cause
    assertThat(failure)
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Map Image Service returned an empty response")
    assertThat(attempts.get()).isEqualTo(1)
  }

  private fun client(response: (Int) -> ClientResponse): MapImageServiceClient = MapImageServiceClient(
    WebClient.builder()
      .exchangeFunction { Mono.just(response(attempts.incrementAndGet())) }
      .build(),
  )

  private fun render(client: MapImageServiceClient): Future<ByteArray> = executor.submit<ByteArray> { client.render(request) }
    .also { result ->
      await().atMost(Duration.ofSeconds(5)).until { scheduler.scheduledTaskCount > 0 || result.isDone }
    }

  private fun advanceToRetry(seconds: Long, expectedAttemptsBeforeRetry: Int) {
    scheduler.advanceTimeBy(Duration.ofSeconds(seconds).minusNanos(1))
    assertThat(attempts.get()).isEqualTo(expectedAttemptsBeforeRetry)
    scheduler.advanceTimeBy(Duration.ofNanos(1))
    assertThat(attempts.get()).isEqualTo(expectedAttemptsBeforeRetry + 1)
  }

  private fun success(): ClientResponse = ClientResponse.create(HttpStatus.OK)
    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE)
    .body("map image")
    .build()

  private fun failure(status: Int, retryAfter: String?): ClientResponse = ClientResponse.create(HttpStatusCode.valueOf(status))
    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    .apply {
      if (retryAfter != null) header(HttpHeaders.RETRY_AFTER, retryAfter)
    }
    .body(if (status == 503) """{"code":"RENDER_BUSY"}""" else """{"message":"Request failed"}""")
    .build()
}
