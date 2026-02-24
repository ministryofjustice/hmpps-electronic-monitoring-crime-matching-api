package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcTemplate
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingRunRepository

@TestConfiguration
class TestFixturesConfig {
  @Bean
  fun crimeMatchingFixtures(
    jdbcTemplate: JdbcTemplate,
    crimeRepository: CrimeRepository,
    crimeVersionRepository: CrimeVersionRepository,
    crimeBatchRepository: CrimeBatchRepository,
    crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository,
    crimeMatchingRunRepository: CrimeMatchingRunRepository,
  ) = CrimeMatchingFixtures(
    jdbcTemplate = jdbcTemplate,
    crimeRepository = crimeRepository,
    crimeVersionRepository = crimeVersionRepository,
    crimeBatchRepository = crimeBatchRepository,
    crimeBatchIngestionAttemptRepository = crimeBatchIngestionAttemptRepository,
    crimeMatchingRunRepository = crimeMatchingRunRepository,
  )
}
