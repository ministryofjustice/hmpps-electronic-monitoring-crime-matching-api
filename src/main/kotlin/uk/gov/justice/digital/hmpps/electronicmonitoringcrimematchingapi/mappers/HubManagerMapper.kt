package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.HubManagerResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.HubManager

@Component
class HubManagerMapper {
  fun toDto(manager: HubManager): HubManagerResponse = HubManagerResponse(
    id = manager.id.toString(),
    name = manager.name,
    hasSignature = manager.signatureImage != null,
  )
}
