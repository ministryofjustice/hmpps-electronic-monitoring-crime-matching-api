package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.EmailAttachmentIngestionError

data class NotifyEmailRequest(
  val type: String,

  @JsonProperty("email_address")
  val emailAddress: String,

  @JsonProperty("reference")
  val reference: String,

  @JsonProperty("ingestion_status")
  val ingestionStatus: IngestionStatus,

  @JsonProperty("file_name")
  val fileName: String,

  @JsonProperty("batch_id")
  val batchId: String,

  @JsonProperty("police_force")
  val policeForce: String,

  @JsonProperty("error_type")
  val errorType: CrimeBatchEmailIngestionErrorType,

  @JsonProperty("records")
  val records: List<CrimeRecordRequest>,

  @JsonProperty("errors")
  val errors: List<EmailAttachmentIngestionError>,

  @JsonProperty("record_count")
  val recordCount: Int,
)
