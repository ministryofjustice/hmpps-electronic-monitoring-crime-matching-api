package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.gnssGaps

private val GNSS_GAP_TRACK_DASH_PATTERN = listOf(8, 8)

internal fun positionTrackLayers(positions: List<Position>): List<MapImageServiceRequest.TrackLayer> = buildList {
  val gaps = gnssGaps(positions)
  var start = 0
  while (start < positions.lastIndex) {
    var end = start + 1
    val dashed = end in gaps
    while (end < positions.lastIndex && (end + 1 in gaps) == dashed) {
      end++
    }

    add(
      MapImageServiceRequest.TrackLayer(
        id = if (isEmpty()) "position-track" else "position-track-${size + 1}",
        points = positions.subList(start, end + 1).map { MapImageServiceRequest.TrackPoint(it.latitude, it.longitude) },
        style = MapImageServiceRequest.TrackStyle(
          stroke = MapImageServiceRequest.TrackStroke(color = "#000000", lineDash = if (dashed) GNSS_GAP_TRACK_DASH_PATTERN else null),
        ),
      ),
    )
    start = end // Adjacent layers share their endpoint.
  }
}
