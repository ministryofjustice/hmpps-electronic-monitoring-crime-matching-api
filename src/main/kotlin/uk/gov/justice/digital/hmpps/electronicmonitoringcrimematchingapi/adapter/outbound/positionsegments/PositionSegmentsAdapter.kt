package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.adapter.outbound.positionsegments

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.positionsegments.PositionSegmentsProvider

@Component
class PositionSegmentsAdapter(
  private val client: PositionSegmentsClient,
) : PositionSegmentsProvider {
  override fun getPositionSegments(deviceId: String, startTime: String, endTime: String): List<List<Position>> = client.get(deviceId, startTime, endTime)
    .positionSegments
}
