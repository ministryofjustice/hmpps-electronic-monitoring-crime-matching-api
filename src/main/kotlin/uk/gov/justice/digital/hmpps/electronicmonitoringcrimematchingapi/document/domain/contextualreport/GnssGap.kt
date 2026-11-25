package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import java.time.Duration
import java.time.Instant

data class GnssGap(val start: Instant, val end: Instant)

/** The key is the index of the position after the gap or the size of the list for a trailing gap. */
/** The value is the gap itself, with start and end times. */
fun gnssGaps(
  positions: List<Position>,
  durationThatIndicatesADataGap: Duration,
  previousTime: Instant? = null,
  nextTime: Instant? = null,
): Map<Int, GnssGap> = buildMap {
  fun recordGapIfAboveThreshold(index: Int, start: Instant, end: Instant) {
    if (Duration.between(start, end) > durationThatIndicatesADataGap) {
      put(index, GnssGap(start, end))
    }
  }

  fun getTimeOfPreviousPosition(index: Int): Instant? = positions.getOrNull(index - 1)?.gpsDate

  positions.forEachIndexed { index, position ->
    val start = getTimeOfPreviousPosition(index) ?: previousTime
    if (start != null) recordGapIfAboveThreshold(index, start, position.gpsDate)
  }
  if (nextTime != null && positions.isNotEmpty()) {
    // Gap between the end of this segment and the start of the next
    recordGapIfAboveThreshold(positions.size, positions.last().gpsDate, nextTime)
  }
}
