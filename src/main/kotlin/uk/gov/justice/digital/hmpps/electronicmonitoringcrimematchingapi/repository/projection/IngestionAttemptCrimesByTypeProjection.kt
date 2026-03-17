package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

interface IngestionAttemptCrimesByTypeProjection {
<<<<<<< Updated upstream
  val crimeType: String?
  val submitted: Int
  val successful: Int
  val failed: Int
=======
  fun getCrimeType(): String?
  fun getFailed(): Long
  fun getSuccessful(): Long
  fun getSubmitted(): Long
>>>>>>> Stashed changes
}
