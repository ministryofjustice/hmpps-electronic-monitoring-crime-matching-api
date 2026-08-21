package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.outbox

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Test configuration providing a [TestClock] bean that allows deterministic time control.
 * Replaces the system clock for integration tests to avoid real-time waits in time-sensitive scenarios.
 */
@TestConfiguration
class TestClockConfig {

  @Bean
  fun testClock(): TestClock = TestClock()

  @Bean
  @Primary
  fun clock(testClock: TestClock): Clock = testClock
}

/**
 * A controllable clock for testing that allows advancing time programmatically.
 * By default behaves like [Clock.systemDefaultZone()], but when [fixedInstant] is set,
 * it always returns that instant (plus any [duration] advances).
 *
 * Use [advanceBy] to simulate time passage without actual waits.
 * Use [reset] to return to real time (useful in @BeforeEach or @AfterEach).
 */
class TestClock(
  private var zoneId: ZoneId = ZoneId.systemDefault(),
) : Clock() {
  private var fixedInstant: Instant? = null

  override fun getZone(): ZoneId = zoneId

  override fun withZone(zone: ZoneId): Clock = TestClock(zone).also {
    it.fixedInstant = fixedInstant
  }

  override fun instant(): Instant = fixedInstant ?: Instant.now()

  /**
   * Returns the current time, either the fixed time (if [advanceBy] was called)
   * or the actual system time.
   */
  fun now(): LocalDateTime = LocalDateTime.ofInstant(instant(), zoneId)

  /**
   * Advances the clock by the given [duration].
   * Initializes [fixedInstant] on first call if it hasn't been set yet.
   */
  fun advanceBy(duration: Duration) {
    val current = fixedInstant ?: Instant.now()
    fixedInstant = current.plus(duration)
  }

  /**
   * Resets the clock to system time. Useful in teardown to prevent leaking test state.
   */
  fun reset() {
    fixedInstant = null
  }

  /**
   * Sets the clock to a specific instant (for precise test setup).
   */
  fun setInstant(instant: Instant) {
    fixedInstant = instant
  }
}
