package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportConfig
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters
import java.time.Duration
import java.time.temporal.ChronoUnit

// Positions are referred to by their index; adjacent layers share the position where the line style changes.
class PositionTrackLayersTest {
  private val config = ContextualReportConfig()

  @Test
  fun `it should return zero layers if there are no positions`() {
    assertThat(positionTrackLayers(emptyList(), config)).isEmpty()
  }

  @Test
  fun `it should return one layer if there are no GNSS gaps`() {
    assertThat(tracksFor(positionsAtMinutes(0, 1, 2))).containsExactly(
      solid(0, 1, 2),
    )
  }

  @Test
  fun `it should return two layers if there is one GNSS gap at the start`() {
    assertThat(tracksFor(positionsAtMinutes(0, 3, 4))).containsExactly(
      dashed(0, 1),
      solid(1, 2),
    )
  }

  @Test
  fun `it should return two layers if there is one GNSS gap at the end`() {
    assertThat(tracksFor(positionsAtMinutes(0, 1, 4))).containsExactly(
      solid(0, 1),
      dashed(1, 2),
    )
  }

  @Test
  fun `it should return three layers if there is one GNSS gap in the middle`() {
    assertThat(tracksFor(positionsAtMinutes(0, 1, 4, 5))).containsExactly(
      solid(0, 1),
      dashed(1, 2),
      solid(2, 3),
    )
  }

  @Test
  fun `it should combine consecutive GNSS gaps into one dashed layer when there are multiple gaps`() {
    assertThat(tracksFor(positionsAtMinutes(0, 1, 4, 5, 8, 11, 12))).containsExactly(
      solid(0, 1),
      dashed(1, 2),
      solid(2, 3),
      dashed(3, 4, 5),
      solid(5, 6),
    )
  }

  @Test
  fun `it should use the configured dash pattern only for intervals exceeding the configured threshold`() {
    val customConfig = ContextualReportConfig(gnssGapThreshold = Duration.ofMinutes(2), gnssGapTrackDashPattern = listOf(4, 6))

    val layers = positionTrackLayers(positionsAtMinutes(0, 2, 5), customConfig)

    assertThat(layers.map { it.style?.stroke?.lineDash }).containsExactly(null, listOf(4, 6))
  }

  private data class Track(val dashed: Boolean, val points: List<MapImageServiceRequest.TrackPoint>)

  private fun solid(vararg positionIndices: Int) = Track(dashed = false, points = positionIndices.map(::trackPointAt))

  private fun dashed(vararg positionIndices: Int) = Track(dashed = true, points = positionIndices.map(::trackPointAt))

  private fun tracksFor(positions: List<Position>) = positionTrackLayers(positions, config).map {
    Track(dashed = !it.style?.stroke?.lineDash.isNullOrEmpty(), points = it.points)
  }

  private fun positionsAtMinutes(vararg minutes: Long): List<Position> {
    val base = givenContextualReportRequestParameters().positionSegments.first().first()

    return minutes.mapIndexed { index, minute ->
      base.copy(
        gpsDate = base.gpsDate.plus(minute, ChronoUnit.MINUTES),
        latitude = latitudeAt(index),
        longitude = longitudeAt(index),
      )
    }
  }

  private fun trackPointAt(index: Int) = MapImageServiceRequest.TrackPoint(latitudeAt(index), longitudeAt(index))

  private fun latitudeAt(index: Int) = 51.5 + index / 1000.0

  private fun longitudeAt(index: Int) = -0.1 - index / 1000.0
}
