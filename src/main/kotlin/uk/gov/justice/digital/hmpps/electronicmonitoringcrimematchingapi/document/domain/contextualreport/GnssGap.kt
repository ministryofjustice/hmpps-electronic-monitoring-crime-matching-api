package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import java.time.Duration
import java.time.Instant

private val MAX_POSITION_INTERVAL = Duration.ofMinutes(1)

data class GnssGap(val start: Instant, val end: Instant)

/** Gaps keyed by the index of the following position, or by size for a trailing gap. */
/** For example, if positions[3] is 10:00 and positions[4] is 10:02, then the gap will be keyed by 4. */
fun gnssGaps(
  positions: List<Position>,
  previousTime: Instant? = null,
  nextTime: Instant? = null,
): Map<Int, GnssGap> = buildMap {
  fun recordGap(index: Int, start: Instant, end: Instant) {
    if (Duration.between(start, end) > MAX_POSITION_INTERVAL) {
      put(index, GnssGap(start, end))
    }
  }

  positions.forEachIndexed { index, position ->
    val start = positions.getOrNull(index - 1)?.gpsDate ?: previousTime
    if (start != null) recordGap(index, start, position.gpsDate)
  }
  if (nextTime != null && positions.isNotEmpty()) {
    recordGap(positions.size, positions.last().gpsDate, nextTime)
  }
}
