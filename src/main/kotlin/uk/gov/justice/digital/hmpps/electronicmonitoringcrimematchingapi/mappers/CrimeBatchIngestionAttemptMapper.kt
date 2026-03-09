package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchIngestionAttemptResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.view.CrimeBatchIngestionAttemptView

@Component
class CrimeBatchIngestionAttemptMapper(
  val ingestionAttemptCrimesByTypeMapper: IngestionAttemptCrimesByTypeMapper,
  val crimeBatchEmailAttachmentErrorMapper: CrimeBatchEmailAttachmentErrorMapper,
) {
  fun toDto(
    view: CrimeBatchIngestionAttemptView,
  ): CrimeBatchIngestionAttemptResponse = CrimeBatchIngestionAttemptResponse(
    ingestionAttemptId = view.ingestionAttempt.ingestionAttemptId,
    ingestionStatus = view.ingestionAttempt.ingestionStatus.name,
    policeForceArea = view.ingestionAttempt.policeForceArea ?: "",
    batchId = view.ingestionAttempt.batchId ?: "",
    matches = view.ingestionAttempt.matches,
    createdAt = view.ingestionAttempt.createdAt.toString(),
    fileName = view.ingestionAttempt.fileName,
    submitted = view.ingestionAttempt.submitted ?: 0,
    successful = view.ingestionAttempt.successful ?: 0,
    failed = view.ingestionAttempt.failed ?: 0,
    crimesByCrimeType = view.crimesByCrimeType.map(ingestionAttemptCrimesByTypeMapper::toDto),
    validationErrors = view.validationErrors.map(crimeBatchEmailAttachmentErrorMapper::toDto),
  )
}
