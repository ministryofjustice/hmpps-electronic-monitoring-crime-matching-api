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
    const val ENABLE_POLICE_CONFIRMATION_EMAILS = "enable-police-confirmation-emails"
  }

  fun policeConfirmationEmailsEnabled() = try {
    enabled(ENABLE_POLICE_CONFIRMATION_EMAILS)
  } catch (e: Exception) {
    logger.warn("Error retrieving feature flag $ENABLE_POLICE_CONFIRMATION_EMAILS, defaulting to false", e)
    false
  }

  private fun enabled(key: String): Boolean = client
    .evaluateBoolean(key, "entityId", emptyMap())
    .isEnabled
}
