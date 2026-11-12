package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.testutils

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport.ContextualReportCrimeMatchingResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport.CrimeMatchingResultStore
import java.util.UUID

class InMemoryCrimeMatchingResultStore(
  private val results: Map<UUID, ContextualReportCrimeMatchingResult>,
) : CrimeMatchingResultStore {
  override fun find(deviceWearerId: UUID): ContextualReportCrimeMatchingResult? = results[deviceWearerId]
}
