package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType

interface CrimeBatchEmailAttachmentErrorProjection {
  val errorType: CrimeBatchEmailAttachmentIngestionErrorType
  val fieldName: String
  val value: String
  val crimeReference: String
  val rowNumber: String
  val crimeType: String
}
