package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingResultRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.ZeroPrecisionCrimeMatchProjection
import java.time.Instant

@Service
class ManagementInformationService(
  private val repo: CrimeMatchingResultRepository,
) {
  fun getZeroPrecisionCrimeMatches(
    from: Instant?,
    to: Instant?,
    page: Int,
    pageSize: Int,
  ): Page<ZeroPrecisionCrimeMatchProjection> = repo.findZeroPrecisionCrimeMatches(
    from,
    to,
    PageRequest.of(
      page,
      pageSize,
    ),
  )
}
