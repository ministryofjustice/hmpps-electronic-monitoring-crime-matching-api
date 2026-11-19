package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

interface FailedCrimeSubmissionProjection {
  val reasonForFailure: String
  val failureCount: Int
}
