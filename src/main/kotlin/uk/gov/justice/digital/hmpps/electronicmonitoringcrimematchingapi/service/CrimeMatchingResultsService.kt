package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingResultRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeMatchingResultProjection

@Service
class CrimeMatchingResultsService(
  private val crimeMatchingResultRepository: CrimeMatchingResultRepository,
) {
  fun getCrimesMatchingResultsForBatches(batchIds: List<String>): List<CrimeMatchingResultProjection> = crimeMatchingResultRepository.findLatestCrimeMatchesByBatchIds(batchIds.toTypedArray())
}
