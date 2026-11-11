package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.adapter.outbound.persistence

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport.ContextualReportCrimeMatchingResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.contextualreport.CrimeMatchingResultStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingResultRepository
import java.time.ZoneOffset
import java.util.UUID

@Component
class CrimeMatchingResultPersistenceAdapter(
  private val repository: CrimeMatchingResultRepository,
) : CrimeMatchingResultStore {
  override fun find(crimeMatchingResultId: UUID, deviceId: Long): ContextualReportCrimeMatchingResult? = repository.findContextualReportCrimeMatchingResult(crimeMatchingResultId, deviceId)?.let {
    ContextualReportCrimeMatchingResult(
      batchId = it.batchId,
      crimeReference = it.crimeReference,
      crimeType = CrimeType.from(it.crimeTypeId).value,
      matchingResultCreatedAt = it.matchingResultCreatedAt
        .atOffset(ZoneOffset.UTC)
        .toInstant(),
      address = it.address,
      dateOfBirth = it.dateOfBirth,
      identifier = it.identifier,
      name = it.name,
      pncRef = it.pncRef,
      deviceWearerId = it.deviceWearerId,
    )
  }
}
