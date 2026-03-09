package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchEmailAttachmentErrorResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeBatchEmailAttachmentErrorProjection

@Component
class CrimeBatchEmailAttachmentErrorMapper {
  fun toDto(ingestionError: CrimeBatchEmailAttachmentErrorProjection): CrimeBatchEmailAttachmentErrorResponse = CrimeBatchEmailAttachmentErrorResponse(
    errorType = ingestionError.errorType.name,
    fieldName = ingestionError.fieldName ?: "",
    value = ingestionError.value ?: "",
    crimeReference = ingestionError.crimeReference ?: "",
    rowNumber = ingestionError.rowNumber,
  )
}
