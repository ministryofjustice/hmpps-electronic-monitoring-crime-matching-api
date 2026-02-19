package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResult

@Service
class CrimeMatchingResultsService {
  fun getCrimesMatchingResultsForBatches(batchIds: List<String>): List<CrimeMatchingResult> = listOf()
}
