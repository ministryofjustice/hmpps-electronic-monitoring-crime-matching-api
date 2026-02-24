package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchIngestionAttemptSummaryResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeBatchIngestionAttemptProjection

@Component
class IngestionAttemptSummaryMapper {
  fun toDto(summary: CrimeBatchIngestionAttemptProjection): CrimeBatchIngestionAttemptSummaryResponse {
    return CrimeBatchIngestionAttemptSummaryResponse(
      ingestionAttemptId = summary.ingestionAttemptId,
      status = summary.ingestionStatus.name,
      policeForce = summary.policeForceArea ?: "",
      batchId = summary.batchId ?: "",
      matches = summary.matches,
      createdAt = summary.createdAt.toString(),
    )
  }
}