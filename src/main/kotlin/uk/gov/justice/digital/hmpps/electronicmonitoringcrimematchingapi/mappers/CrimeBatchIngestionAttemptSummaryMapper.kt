package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchIngestionAttemptSummaryResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeBatchIngestionAttemptSummaryProjection

@Component
class CrimeBatchIngestionAttemptSummaryMapper {
  fun toDto(projection: CrimeBatchIngestionAttemptSummaryProjection): CrimeBatchIngestionAttemptSummaryResponse = CrimeBatchIngestionAttemptSummaryResponse(
    ingestionAttemptId = projection.getIngestionAttemptId().toString(),
    ingestionStatus = projection.getIngestionStatus(),
    policeForceArea = projection.getPoliceForceArea(),
    crimeBatchId = projection.getCrimeBatchId()?.toString(),
    batchId = projection.getBatchId(),
    matches = projection.getMatches(),
    createdAt = projection.getCreatedAt().toString(),
  )
}
