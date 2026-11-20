package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType

interface CrimeSubmissionSummaryProjection {
  val crimeType: CrimeType?
  val submitted: Long
  val ingested: Long
  val failed: Long
  val notIngestedPercentage: Double
}
