package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.domain.positionsegments

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position

interface PositionSegmentsProvider {
  fun getPositionSegments(deviceActivationId: String, startTime: String, endTime: String): List<List<Position>>
}
