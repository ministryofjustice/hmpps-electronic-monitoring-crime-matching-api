package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters
import java.time.temporal.ChronoUnit

class PositionTrackLayersTest {
  @Test
  fun `it should return zero layers if there are no positions`() {
    assertThat(positionTrackLayers(emptyList())).isEmpty()
  }

  @Test
  fun `it should return one layer if there are no GNSS gaps`() {
    val layers = positionTrackLayers(positionsAtMinutes(0, 1, 2))

    assertThat(lineStyles(layers)).containsExactly("solid")
  }

  @Test
  fun `it should return two layers if there is one GNSS gap at the start`() {
    val layers = positionTrackLayers(positionsAtMinutes(0, 3, 4))

    assertThat(lineStyles(layers)).containsExactly("dashed", "solid")
  }

  @Test
  fun `it should return two layers if there is one GNSS gap at the end`() {
    val layers = positionTrackLayers(positionsAtMinutes(0, 1, 4))

    assertThat(lineStyles(layers)).containsExactly("solid", "dashed")
  }

  @Test
  fun `it should return three layers if there is one GNSS gap in the middle`() {
    val layers = positionTrackLayers(positionsAtMinutes(0, 1, 4, 5))

    assertThat(lineStyles(layers)).containsExactly("solid", "dashed", "solid")
  }

  private fun positionsAtMinutes(vararg minutes: Long) = with(givenContextualReportRequestParameters().positionSegments.first().first()) {
    minutes.map { copy(gpsDate = gpsDate.plus(it, ChronoUnit.MINUTES)) }
  }

  private fun lineStyles(layers: List<MapImageServiceRequest.TrackLayer>) = layers.map {
    if (it.style?.stroke?.lineDash.isNullOrEmpty()) "solid" else "dashed"
  }
}
