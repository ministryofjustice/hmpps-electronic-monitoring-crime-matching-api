package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchIngestionAttemptResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttemptSummary

@Component
class CrimeBatchIngestionAttemptMapper(
  val ingestionAttemptCrimesByTypeMapper: IngestionAttemptCrimesByTypeMapper,
  val crimeBatchEmailAttachmentErrorMapper: CrimeBatchEmailAttachmentErrorMapper,
) {
  fun toDto(
    summary: CrimeBatchIngestionAttemptSummary,
  ): CrimeBatchIngestionAttemptResponse = CrimeBatchIngestionAttemptResponse(
    ingestionAttemptId = summary.ingestionAttempt.ingestionAttemptId,
    ingestionStatus = summary.ingestionAttempt.ingestionStatus.name,
    policeForceArea = summary.ingestionAttempt.policeForceArea ?: "",
    batchId = summary.ingestionAttempt.batchId ?: "",
    matches = summary.ingestionAttempt.matches,
    createdAt = summary.ingestionAttempt.createdAt.toString(),
    fileName = summary.ingestionAttempt.fileName,
    submitted = summary.ingestionAttempt.submitted ?: 0,
    successful = summary.ingestionAttempt.successful ?: 0,
    failed = summary.ingestionAttempt.failed ?: 0,
    crimesByCrimeType = summary.crimesByCrimeType.map(ingestionAttemptCrimesByTypeMapper::toDto),
    validationErrors = summary.validationErrors.map(crimeBatchEmailAttachmentErrorMapper::toDto),
  )
}
