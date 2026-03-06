package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.proximityAlert

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeVersionSummaryProjection

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
    pageable =
    PageRequest.of(
      page,
      pageSize,
      Sort.by(Sort.Direction.DESC, "ingestionDateTime"),
    ),
  )
}
