package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.adapter.outbound.positionsegments

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position

data class PositionSegmentsResponse(
  val segmentationVersion: String,
  val positionSegments: List<List<Position>>,
)
