package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.testutils

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.positionsegments.PositionSegmentsProvider

class InMemoryPositionSegmentsProvider(var positionSegments: List<List<Position>>) : PositionSegmentsProvider {
  var lastRequest: PositionSegmentsRequest? = null
    private set

  override fun getPositionSegments(deviceId: String, startTime: String, endTime: String): List<List<Position>> {
    lastRequest = PositionSegmentsRequest(deviceId, startTime, endTime)
    return positionSegments
  }
}
