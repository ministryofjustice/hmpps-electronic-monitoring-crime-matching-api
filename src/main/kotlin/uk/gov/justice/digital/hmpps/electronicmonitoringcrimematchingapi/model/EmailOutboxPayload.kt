package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.EmailAttachmentIngestionError

/**
 * Self-contained snapshot of everything the email-send worker needs to rebuild and send the
 * GOV.UK Notify email for a single recipient of an ingestion outcome. Persisted as the
 * `email_outbox.payload`. One row (and therefore one payload) exists per recipient, so each
 * recipient is tracked independently.
 */
data class EmailOutboxPayload(
  val schemaVersion: Int = 1,
  val ingestionStatus: IngestionStatus,
  val recipient: String,
  val fileName: String,
  val batchId: String,
  val crimeBatchId: String,
  val policeForce: String,
  val errorType: CrimeBatchEmailIngestionErrorType,
  val recordCount: Int,
  val records: List<CrimeRecordRequest> = emptyList(),
  val errors: List<EmailAttachmentIngestionError> = emptyList(),
)
