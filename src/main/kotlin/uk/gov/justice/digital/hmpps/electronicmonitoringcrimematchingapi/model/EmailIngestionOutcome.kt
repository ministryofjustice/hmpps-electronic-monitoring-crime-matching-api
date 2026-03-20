package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.EmailAttachmentIngestionError

data class EmailIngestionOutcome(
  val batchId: String = "Unknown due to an error",
  val policeForce: String = "Unknown due to an error",
  val errorType: CrimeBatchEmailIngestionErrorType = CrimeBatchEmailIngestionErrorType.UNKNOWN,
  val errors: List<EmailAttachmentIngestionError> = emptyList(),
  val emailData: EmailData,
  val records: List<CrimeRecordRequest> = emptyList(),
  val recordCount: Int = 0,
  val ingestionStatus: IngestionStatus,
)
