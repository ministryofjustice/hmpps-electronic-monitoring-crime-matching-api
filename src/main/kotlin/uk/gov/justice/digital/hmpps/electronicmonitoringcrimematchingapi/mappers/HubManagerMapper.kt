package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.HubManagerResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.HubManager

class HubManagerMapper {
  fun toDto(manager: HubManager): HubManagerResponse = HubManagerResponse(
    name = manager.name,
    hasSignature = manager.signatureImage != null,
  )
}
