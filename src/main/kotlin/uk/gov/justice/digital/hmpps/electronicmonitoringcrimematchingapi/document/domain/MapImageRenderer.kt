package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

interface MapImageRenderer {
  fun render(request: MapImageRequest): ByteArray
}
