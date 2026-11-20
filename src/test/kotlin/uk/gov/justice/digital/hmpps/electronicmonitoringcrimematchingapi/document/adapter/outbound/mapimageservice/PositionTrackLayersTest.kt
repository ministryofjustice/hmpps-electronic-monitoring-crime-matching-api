package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters

class PositionTrackLayersTest {
  @Test
  fun `only edges longer than a minute are dashed and layers share their endpoints`() {
    val first = givenContextualReportRequestParameters().positionSegments.first().first()
    val positions = listOf(0L, 60, 121, 190, 220).mapIndexed { index, seconds ->
      first.copy(gpsDate = first.gpsDate.plusSeconds(seconds), latitude = 51.0 + index)
    }

    val tracks = positionTrackLayers(positions)

    assertThat(tracks.map { it.id }).doesNotHaveDuplicates()
    assertThat(tracks.map { it.style?.stroke?.lineDash }).containsExactly(null, listOf(8, 8), null)
    assertThat(tracks.map { layer -> layer.points.map { it.latitude } }).containsExactly(
      listOf(51.0, 52.0),
      listOf(52.0, 53.0, 54.0),
      listOf(54.0, 55.0),
    )
  }
}
