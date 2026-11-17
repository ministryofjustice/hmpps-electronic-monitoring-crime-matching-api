package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position

data class MapImageRequest(
  val positions: List<Position>,
)
