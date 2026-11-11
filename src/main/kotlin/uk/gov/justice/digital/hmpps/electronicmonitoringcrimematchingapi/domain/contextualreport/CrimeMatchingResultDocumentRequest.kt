package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport

import java.util.UUID

data class CrimeMatchingResultDocumentRequest(
  val documentRequestId: UUID,
  val crimeMatchingResultDeviceWearerId: UUID,
)
