package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachmentIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType

class CrimeBatchEmailAttachmentContext(
  private val crimeBatchEmailAttachment: CrimeBatchEmailAttachment,
) {
  fun withAttachmentIngestionError(
    crimeReference: String? = null,
    fieldName: String? = "crimeReference",
    errorType: CrimeBatchEmailAttachmentIngestionErrorType = CrimeBatchEmailAttachmentIngestionErrorType.MISSING_CRIME_REFERENCE,
  ) {
    crimeBatchEmailAttachment.crimeBatchEmailAttachmentIngestionErrors.add(
      CrimeBatchEmailAttachmentIngestionError(
        rowNumber = 1,
        crimeReference = crimeReference,
        fieldName = fieldName,
        value = null,
        errorType = errorType,
        crimeTypeId = null,
        crimeBatchEmailAttachment = crimeBatchEmailAttachment,
      ),
    )
  }
}
