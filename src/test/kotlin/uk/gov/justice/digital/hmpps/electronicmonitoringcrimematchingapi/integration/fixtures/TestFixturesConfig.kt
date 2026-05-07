package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingRunRepository

@TestConfiguration
class TestFixturesConfig {
  @Bean
  fun crimeMatchingFixtures(
    crimeRepository: CrimeRepository,
    crimeVersionRepository: CrimeVersionRepository,
    crimeBatchRepository: CrimeBatchRepository,
    crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository,
    crimeMatchingRunRepository: CrimeMatchingRunRepository,
  ) = CrimeMatchingFixtures(
    crimeRepository = crimeRepository,
    crimeVersionRepository = crimeVersionRepository,
    crimeBatchRepository = crimeBatchRepository,
    crimeBatchIngestionAttemptRepository = crimeBatchIngestionAttemptRepository,
    crimeMatchingRunRepository = crimeMatchingRunRepository,
  )
}
