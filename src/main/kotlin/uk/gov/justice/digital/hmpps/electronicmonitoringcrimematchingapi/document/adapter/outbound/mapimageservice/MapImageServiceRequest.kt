package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.adapter.outbound.mapimageservice

import com.fasterxml.jackson.annotation.JsonInclude

// The Map Image Service rejects nulls for optional fields, so nulls must never be serialised
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MapImageServiceRequest(
  val viewport: Viewport,
  // TODO(ACR-1106): Add layers (e.g. tracks, positions) and output options when required
) {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class Viewport(
    val centre: Coordinate,
    val zoom: Int,
  ) {
    val mode = "explicit"
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  data class Coordinate(
    val latitude: Double,
    val longitude: Double,
  )
}
