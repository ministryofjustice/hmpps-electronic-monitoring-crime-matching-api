package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachmentIngestionError
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType

class CrimeBatchIngestionAttemptContext(
  private val crimeBatchEmailAttachment: CrimeBatchEmailAttachment,
) {
  fun withAttachmentIngestionError() {
    crimeBatchEmailAttachment.crimeBatchEmailAttachmentIngestionErrors.add(
      CrimeBatchEmailAttachmentIngestionError(
        rowNumber = 1,
        crimeReference = null,
        fieldName = "crimeReference",
        value = null,
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.MISSING_CRIME_REFERENCE,
        crimeTypeId = null,
        crimeBatchEmailAttachment = crimeBatchEmailAttachment,
      ),
    )
  }
}
