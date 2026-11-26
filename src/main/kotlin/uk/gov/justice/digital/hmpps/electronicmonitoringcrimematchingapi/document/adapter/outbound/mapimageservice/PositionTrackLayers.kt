package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportConfig
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.GnssGap
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.gnssGaps

/** Returns a list of track layers, each of which is either solid or dashed, the latter representing gaps in the GNSS data. */
/** Each track layer of the Map Image Service supports a single line type only, so we need multiple track layers if multiple styles are required. **/
/** A new track layer is started whenever a gap is encountered. **/
internal fun positionTrackLayers(positions: List<Position>, config: ContextualReportConfig): List<MapImageServiceRequest.TrackLayer> = buildList {
  val gaps = gnssGaps(positions, config.gnssGapThreshold)
  var pair = PositionPair(0, 0)
  while (pair.start < positions.lastIndex) {
    pair = pair.copy(end = pair.start + 1)
    val isDashed = pair.end in gaps
    while (pair.end < positions.lastIndex && nextPositionIsSameTrackStyle(pair.end + 1, gaps, isDashed)) {
      pair = pair.copy(end = pair.end + 1)
    }

    add(
      MapImageServiceRequest.TrackLayer(
        id = if (isEmpty()) "position-track" else "position-track-${size + 1}",
        points = positions.subList(pair.start, pair.end + 1).map { MapImageServiceRequest.TrackPoint(it.latitude, it.longitude) },
        style = MapImageServiceRequest.TrackStyle(
          stroke = MapImageServiceRequest.TrackStroke(color = "#000000", lineDash = if (isDashed) config.gnssGapTrackDashPattern else null),
        ),
      ),
    )
    pair = pair.copy(start = pair.end) // The last position in a track layer is also the first position of the next track layer.
  }
}

private fun nextPositionIsSameTrackStyle(
  nextIndex: Int,
  gaps: Map<Int, GnssGap>,
  isDashed: Boolean,
): Boolean = (nextIndex in gaps) == isDashed

private data class PositionPair(val start: Int, val end: Int)
