package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.MatchingNotificationService

@Service
class OutboxScheduler(
  private val matchingNotificationService: MatchingNotificationService,
  private val emailNotificationService: EmailNotificationService,
) {
  companion object {
    const val EVERY_15_MINUTES: String = "PT15M"
    const val ONE_MINUTE: String = "PT1M"
  }

  @Scheduled(fixedRateString = EVERY_15_MINUTES)
  fun publishMatchingRequestsScheduled() {
    matchingNotificationService.publishMatchingRequests()
  }

  @Scheduled(fixedRateString = EVERY_15_MINUTES, initialDelayString = ONE_MINUTE)
  fun notifyEmailRequestsScheduled() {
    emailNotificationService.sendEmails()
  }
}
