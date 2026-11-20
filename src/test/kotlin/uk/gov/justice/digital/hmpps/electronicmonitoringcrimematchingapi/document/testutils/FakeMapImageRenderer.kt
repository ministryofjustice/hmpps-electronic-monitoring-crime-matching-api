package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.MapImageRenderer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.MapImageRequest

class FakeMapImageRenderer : MapImageRenderer {
  val requests = mutableListOf<MapImageRequest>()

  override fun render(request: MapImageRequest): ByteArray {
    requests.add(request)
    return requireNotNull(
      javaClass.getResourceAsStream("/contextualreport/explanatory-map.jpg"),
    ).use { it.readBytes() }
  }
}
