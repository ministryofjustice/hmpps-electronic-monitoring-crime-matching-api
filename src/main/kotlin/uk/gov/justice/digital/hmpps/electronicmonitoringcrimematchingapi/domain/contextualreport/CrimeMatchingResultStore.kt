package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport

import java.util.UUID

interface CrimeMatchingResultStore {
  fun find(
    crimeMatchingResultId: UUID,
    deviceId: Long,
  ): ContextualReportCrimeMatchingResult?
}
