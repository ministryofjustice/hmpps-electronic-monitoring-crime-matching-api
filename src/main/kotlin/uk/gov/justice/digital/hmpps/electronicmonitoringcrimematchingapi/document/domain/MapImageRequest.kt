package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

// TODO(ACR-1106): Add map layers (e.g. tracks, positions, confidence circles) once required
data class MapImageRequest(
  val centreLatitude: Double,
  val centreLongitude: Double,
  val zoom: Int,
)
