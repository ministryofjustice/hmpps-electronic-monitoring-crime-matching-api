package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchEmailAttachmentIngestionErrorRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingResultRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.FailedCrimeSubmissionProjection
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.ZeroPrecisionCrimeMatchProjection
import java.time.Instant

@Service
class ManagementInformationService(
  private val crimeMatchingResultRepository: CrimeMatchingResultRepository,
  private val crimeBatchEmailAttachmentIngestionErrorRepository: CrimeBatchEmailAttachmentIngestionErrorRepository,
) {
  fun getZeroPrecisionCrimeMatches(
    from: Instant?,
    to: Instant?,
    page: Int?,
    pageSize: Int,
  ): Page<ZeroPrecisionCrimeMatchProjection> {
    val pageable = page?.let {
      PageRequest.of(
        page,
        pageSize,
      )
    } ?: Pageable.unpaged()

    return crimeMatchingResultRepository.findZeroPrecisionCrimeMatches(
      from,
      to,
      pageable,
    )
  }

  fun getFailedCrimeSubmissions(
    policeForceArea: PoliceForce?,
    from: Instant?,
    to: Instant?,
  ): List<FailedCrimeSubmissionProjection> = crimeBatchEmailAttachmentIngestionErrorRepository.findFailedCrimeSubmissions(policeForceArea?.name, from, to)
}
