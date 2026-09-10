package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.MatchingNotificationService

@Service
class MatchingNotificationScheduler(
  private val matchingNotificationService: MatchingNotificationService,
) {
  companion object {
    const val EVERY_15_MINUTES: String = "PT15M"
  }

  @Scheduled(fixedRateString = EVERY_15_MINUTES)
  fun publishMatchingRequestsScheduled() {
    matchingNotificationService.publishMatchingRequests()
  }
}
