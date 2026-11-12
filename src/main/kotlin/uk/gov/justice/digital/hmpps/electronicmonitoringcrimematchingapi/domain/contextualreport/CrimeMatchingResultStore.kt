package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport

import java.util.UUID

interface CrimeMatchingResultStore {
  fun find(
    deviceWearerId: UUID,
  ): ContextualReportCrimeMatchingResult?
}
