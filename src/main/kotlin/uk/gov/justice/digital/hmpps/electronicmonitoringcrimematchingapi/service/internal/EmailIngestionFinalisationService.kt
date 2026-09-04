package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchEmailIngestionService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchService

@Service
class EmailIngestionFinalisationService(
  private val crimeBatchEmailIngestionService: CrimeBatchEmailIngestionService,
  private val crimeBatchService: CrimeBatchService,
) {
  @Transactional
  fun persistIngestion(preparation: EmailIngestionPreparation): EmailIngestionOutcome {
    val persistedAttempt = crimeBatchEmailIngestionService.saveCrimeBatchIngestionAttempt(preparation.crimeBatchIngestionAttempt)
    val outcome = preparation.ingestionOutcome

    if (outcome.ingestionStatus == IngestionStatus.SUCCESSFUL || outcome.ingestionStatus == IngestionStatus.PARTIAL) {
      val attachment = persistedAttempt.crimeBatchEmail
        ?.crimeBatchEmailAttachments
        ?.singleOrNull()
        ?: throw IllegalStateException("Expected exactly one persisted email attachment for successful ingestion")

      val crimeBatch = crimeBatchService.createCrimeBatch(outcome.records, attachment)

      return outcome.copy(
        batchId = crimeBatch.batchId,
        crimeBatchId = crimeBatch.id.toString(),
      )
    }

    return outcome
  }
}
