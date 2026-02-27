package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchIngestionAttemptSummaryResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeBatchIngestionAttemptSummaryProjection

@Component
class CrimeBatchIngestionAttemptSummaryMapper {
  fun toDto(summary: CrimeBatchIngestionAttemptSummaryProjection): CrimeBatchIngestionAttemptSummaryResponse = CrimeBatchIngestionAttemptSummaryResponse(
    ingestionAttemptId = summary.ingestionAttemptId,
    ingestionStatus = summary.ingestionStatus.name,
    policeForceArea = summary.policeForceArea ?: "",
    batchId = summary.batchId ?: "",
    matches = summary.matches,
    createdAt = summary.createdAt.toString(),
  )
}
