package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.EmailAttachmentIngestionError

class ParseResult(
  val recordCount: Int,
  val records: List<CrimeRecordDto>,
  val errors: List<EmailAttachmentIngestionError>,
)
