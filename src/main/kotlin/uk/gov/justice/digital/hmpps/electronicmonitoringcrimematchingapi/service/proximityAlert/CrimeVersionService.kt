package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.proximityAlert

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeVersionProjection
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeVersionSummaryProjection
import java.util.UUID

@Service
class CrimeVersionService(
  private val crimeVersionRepository: CrimeVersionRepository,
) {

  fun searchCrimeVersions(
    crimeRef: String,
    page: Int,
    pageSize: Int,
  ): Page<CrimeVersionSummaryProjection> = crimeVersionRepository.findCrimeVersionsByCrimeReference(
    crimeReference = crimeRef,
    pageable = PageRequest.of(page, pageSize),
  )

  fun getCrimeVersion(id: UUID): List<CrimeVersionProjection> {
    val results = crimeVersionRepository.findCrimeVersionMatchingResult(id)

    if (results.isEmpty()) {
      throw EntityNotFoundException("No crime version found with id: $id")
    }

    return results
  }
}
