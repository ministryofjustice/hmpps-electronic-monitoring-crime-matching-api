package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportConfig
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.MapImageRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters
import java.time.Instant

class MapImageServiceAdapterTest {
  private val server = WireMockServer(wireMockConfig().dynamicPort())
  private val config = ContextualReportConfig()
  private val segmentPositions = givenContextualReportRequestParameters().positionSegments.single()

  @BeforeEach
  fun setUp() {
    server.start()
    server.stubFor(post("/map-image").willReturn(aResponse().withHeader("Content-Type", "image/png").withBody("image")))
  }

  @AfterEach
  fun tearDown() {
    server.stop()
  }

  @Test
  fun `sends fit viewport padding when requested`() {
    val viewport = viewportSentFor(MapImageRequest(segmentPositions, viewportPaddingPx = 80))

    assertThat(viewport.getString("mode")).isEqualTo("fit")
    assertThat(viewport.getInt("padding")).isEqualTo(80)
  }

  @Test
  fun `omits fit viewport padding when not requested`() {
    val viewport = viewportSentFor(MapImageRequest(segmentPositions))

    assertThat(viewport.getString("mode")).isEqualTo("fit")
    assertThat(viewport.has("padding")).isFalse()
  }

  @ParameterizedTest
  @ValueSource(ints = [1, 2])
  fun `sends styled positions and tracks in the existing layer order`(count: Int) {
    val client = MapImageServiceClient(WebClient.builder().baseUrl(server.baseUrl()).build())
    val positions = (1..count).map {
      Position(
        direction = 0,
        gpsDate = Instant.parse("2025-01-01T00:00:00Z"),
        latitude = 51.5 + it / 100.0,
        longitude = -0.1,
        precision = 20,
        speed = 0,
        sequenceNumber = it,
      )
    }

    assertThat(MapImageServiceAdapter(client, config).render(MapImageRequest(positions))).isEqualTo("image".toByteArray())

    val request = JSONObject(server.allServeEvents.single().request.bodyAsString)
    val controls = request.getJSONObject("controls")

    assertThat(controls.getBoolean("compass")).isTrue()
    assertThat(controls.has("scaleBar")).isFalse()
    val layers = request.getJSONArray("layers")
    val expectedIds = if (count > 1) {
      listOf("position-track", "position-confidence-circles", "position-locations", "position-sequence-numbers")
    } else {
      listOf("position-confidence-circles", "position-locations", "position-sequence-numbers")
    }
    assertThat((0 until layers.length()).map { layers.getJSONObject(it).getString("id") }).isEqualTo(expectedIds)

    if (count > 1) {
      val track = layers.getJSONObject(0)
      assertThat(track.getString("type")).isEqualTo("tracks")
      assertThat(track.getJSONObject("style").getJSONObject("stroke").getString("color")).isEqualTo("#000000")
    }

    val circle = layers.getJSONObject(if (count > 1) 1 else 0)
    assertThat(circle.getString("type")).isEqualTo("circles")
    assertThat(circle.has("zIndex")).isFalse()
    assertThat(circle.getJSONArray("points").length()).isEqualTo(count)
    val style = circle.getJSONObject("style")
    assertThat(style.has("fill")).isTrue()
    assertThat(style.isNull("fill")).isTrue()
    val stroke = style.getJSONObject("stroke")
    assertThat(stroke.getString("color")).isEqualTo("#f2a93b")
    assertThat(stroke.getInt("width")).isEqualTo(2)
    assertThat(stroke.getJSONArray("lineDash").toList()).containsExactly(8, 3)

    val locations = layers.getJSONObject(if (count > 1) 2 else 1)
    assertThat(locations.getString("type")).isEqualTo("locations")
    val locationStyle = locations.getJSONObject("style")
    assertThat(locationStyle.getString("fill")).isEqualTo("#f2a93b")
    assertThat(locationStyle.getJSONObject("stroke").getString("color")).isEqualTo("#00000000")
  }

  private fun viewportSentFor(request: MapImageRequest): JSONObject {
    val client = MapImageServiceClient(WebClient.builder().baseUrl(server.baseUrl()).build())
    MapImageServiceAdapter(client, config).render(request)
    return JSONObject(server.allServeEvents.single().request.bodyAsString).getJSONObject("viewport")
  }
}
