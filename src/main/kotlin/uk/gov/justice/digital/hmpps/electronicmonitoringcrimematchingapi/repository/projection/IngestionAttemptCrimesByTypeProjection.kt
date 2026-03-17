package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

interface IngestionAttemptCrimesByTypeProjection {
  val crimeType: String?
  val submitted: Int
  val successful: Int
  val failed: Int
}
