package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class GnssGapTest {
  private val threshold = Duration.ofMinutes(1)

  @Test
  fun `it should return an empty map when there are no gaps`() {
    val positions = listOf(positionAt("10:01:00"), positionAt("10:02:00"), positionAt("10:02:30"))

    val gaps = gnssGaps(positions, threshold, previousTime = time("10:00:00"), nextTime = time("10:03:30"))

    assertThat(gaps).isEmpty()
  }

  @Test
  fun `it should include a leading gap at index 0 when more than a minute passes between previousTime and the first position`() {
    val previousTime = time("10:00:00")
    val first = positionAt("10:03:00")

    val gaps = gnssGaps(listOf(first), threshold, previousTime = previousTime)

    assertThat(gaps).isEqualTo(mapOf(0 to GnssGap(previousTime, first.gpsDate)))
  }

  @Test
  fun `it should record each gap between positions at the index of the position after the gap`() {
    val first = positionAt("10:00:00")
    val second = positionAt("10:01:01")
    val third = positionAt("10:02:01")
    val fourth = positionAt("10:05:00")

    val gaps = gnssGaps(listOf(first, second, third, fourth), threshold)

    assertThat(gaps).isEqualTo(
      mapOf(
        1 to GnssGap(first.gpsDate, second.gpsDate),
        3 to GnssGap(third.gpsDate, fourth.gpsDate),
      ),
    )
  }

  @Test
  fun `it should include a trailing gap at the size of the list when more than a minute passes between the last position and nextTime`() {
    val first = positionAt("10:00:00")
    val last = positionAt("10:01:00")
    val nextTime = time("10:04:00")

    val gaps = gnssGaps(listOf(first, last), threshold, nextTime = nextTime)

    assertThat(gaps).isEqualTo(mapOf(2 to GnssGap(last.gpsDate, nextTime)))
  }

  private fun time(clockTime: String): Instant = Instant.parse("2026-01-15T${clockTime}Z")

  private fun positionAt(clockTime: String) = Position(
    gpsDate = time(clockTime),
    latitude = 51.5,
    longitude = -0.1,
    precision = 5,
    direction = 0,
    speed = 0,
    sequenceNumber = 1,
  )
}
