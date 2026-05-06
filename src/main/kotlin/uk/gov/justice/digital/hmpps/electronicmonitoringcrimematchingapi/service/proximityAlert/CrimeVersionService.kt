package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.proximityAlert

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
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

  fun getCrimeVersion(id: UUID): CrimeVersion {
    val crimeVersion = crimeVersionRepository.findById(id).orElseThrow {
      EntityNotFoundException("No crime version found with id: $id")
    }
    return crimeVersion
  }
}
