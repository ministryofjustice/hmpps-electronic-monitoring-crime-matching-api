package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchEmailAttachmentErrorResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeBatchEmailAttachmentErrorProjection

@Component
class CrimeBatchEmailAttachmentErrorMapper {
  fun toDto(ingestionError: CrimeBatchEmailAttachmentErrorProjection): CrimeBatchEmailAttachmentErrorResponse = CrimeBatchEmailAttachmentErrorResponse(
    crimeReference = getCrimeReference(ingestionError),
    errorType = ingestionError.errorType.message,
    requiredAction = ingestionError.errorType.requiredAction,
  )

  private fun getCrimeReference(ingestionError: CrimeBatchEmailAttachmentErrorProjection): String {
    if (ingestionError.crimeReference == null || ingestionError.crimeReference === "") {
      return "Missing (line: ${ingestionError.rowNumber})"
    }

    return ingestionError.crimeReference!!
  }
}
