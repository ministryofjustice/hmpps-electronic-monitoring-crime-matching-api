package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.AttachmentIngestionErrorResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.FailedCrimeSubmissionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.FailedCrimeSubmissionProjection

@Component
class FailedCrimeSubmissionMapper {
  fun toDto(
    results: List<FailedCrimeSubmissionProjection>,
  ): FailedCrimeSubmissionResponse {
    val countsByType = results.associate {
      it.reasonForFailure to it.failureCount
    }

    return FailedCrimeSubmissionResponse(
      total = results.sumOf { it.failureCount.toLong() },
      failures = CrimeBatchEmailAttachmentIngestionErrorType.entries.map { type ->
        AttachmentIngestionErrorResponse(
          reasonForFailure = type.message,
          failureCount = countsByType[type.name] ?: 0,
        )
      },
    )
  }
}
