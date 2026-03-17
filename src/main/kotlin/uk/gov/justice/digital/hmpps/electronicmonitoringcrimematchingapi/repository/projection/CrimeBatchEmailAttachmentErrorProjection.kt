package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection

<<<<<<< Updated upstream
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType

interface CrimeBatchEmailAttachmentErrorProjection {
  val errorType: CrimeBatchEmailAttachmentIngestionErrorType
  val fieldName: String?
  val value: String?
  val crimeReference: String?
  val rowNumber: Long
  val crimeType: String?
=======
interface CrimeBatchEmailAttachmentErrorProjection {
  fun getErrorType(): String
  fun getFieldName(): String?
  fun getValue(): String?
  fun getCrimeReference(): String?
  fun getRowNumber(): Long
  fun getCrimeType(): String?
>>>>>>> Stashed changes
}
