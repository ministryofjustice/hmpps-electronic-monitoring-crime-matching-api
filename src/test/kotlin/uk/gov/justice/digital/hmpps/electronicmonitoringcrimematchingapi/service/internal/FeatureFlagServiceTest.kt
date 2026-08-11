package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import io.flipt.client.FliptClient
import io.flipt.client.FliptException
import io.flipt.client.models.BooleanEvaluationResponse
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class FeatureFlagServiceTest {
  private lateinit var featureFlagService: FeatureFlagService
  private val fliptClient: FliptClient = mock()
  private val response: BooleanEvaluationResponse = mock()

  @BeforeEach
  fun setup() {
    featureFlagService = FeatureFlagService(fliptClient)
  }

  @Test
  fun `it should return true when the feature flag is enabled`() {
    whenever(fliptClient.evaluateBoolean(FeatureFlagService.ENABLE_POLICE_CONFIRMATION_EMAILS, "entityId", emptyMap()))
      .thenReturn(response)
    whenever(response.isEnabled).thenReturn(true)

    assertTrue(featureFlagService.policeConfirmationEmailsEnabled())
  }

  @Test
  fun `it should return false when the feature flag is disabled`() {
    whenever(fliptClient.evaluateBoolean(FeatureFlagService.ENABLE_POLICE_CONFIRMATION_EMAILS, "entityId", emptyMap()))
      .thenReturn(response)
    whenever(response.isEnabled).thenReturn(false)

    assertFalse(featureFlagService.policeConfirmationEmailsEnabled())
  }

  @Test
  fun `it should return false when the flag can't be retrieved`() {
    whenever(fliptClient.evaluateBoolean(FeatureFlagService.ENABLE_POLICE_CONFIRMATION_EMAILS, "entityId", emptyMap()))
      .thenThrow(FliptException::class.java)

    assertFalse(featureFlagService.policeConfirmationEmailsEnabled())
  }
}
