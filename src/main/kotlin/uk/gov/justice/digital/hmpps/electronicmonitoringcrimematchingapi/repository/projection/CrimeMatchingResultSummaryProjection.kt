package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce

interface CrimeMatchingResultSummaryProjection {
  val policeForceArea: PoliceForce
  val batchId: String
  val automatedMatches: Long
}
