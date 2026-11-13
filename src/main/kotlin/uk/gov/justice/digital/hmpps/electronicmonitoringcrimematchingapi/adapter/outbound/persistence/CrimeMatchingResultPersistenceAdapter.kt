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
  override fun find(deviceWearerId: UUID): ContextualReportCrimeMatchingResult? = repository.findContextualReportCrimeMatchingResult(deviceWearerId)?.let {
    ContextualReportCrimeMatchingResult(
      batchId = it.batchId,
      crimeReference = it.crimeReference,
      crimeType = CrimeType.from(it.crimeTypeId).value,
      crimeDateTimeFrom = it.crimeDateTimeFrom
        .atOffset(ZoneOffset.UTC)
        .toInstant(),
      crimeDateTimeTo = it.crimeDateTimeTo
        .atOffset(ZoneOffset.UTC)
        .toInstant(),
      matchingResultCreatedAt = it.matchingResultCreatedAt
        .atOffset(ZoneOffset.UTC)
        .toInstant(),
      address = it.address,
      dateOfBirth = it.dateOfBirth,
      identifier = it.identifier,
      name = it.name,
      pncRef = it.pncRef,
      deviceId = it.deviceId,
    )
  }
}
