package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import io.flipt.client.FliptClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FeatureFlagService(
  private val client: FliptClient,
) {
  companion object {
    private val logger = LoggerFactory.getLogger(this::class.java)
    const val ENABLE_POLICE_EMAIL_NOTIFICATIONS = "enable-police-email-notifications"
  }

  fun enabled(key: String) = try {
    client
      .evaluateBoolean(key, "entityId", emptyMap())
      .isEnabled
  } catch (e: Exception) {
    logger.warn("Error retrieving feature flag $key, defaulting to false", e)
    false
  }
}
