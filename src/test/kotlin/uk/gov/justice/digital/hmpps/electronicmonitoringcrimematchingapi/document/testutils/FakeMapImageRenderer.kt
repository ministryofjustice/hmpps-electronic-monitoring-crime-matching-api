package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.MapImageRenderer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.MapImageRequest

class FakeMapImageRenderer : MapImageRenderer {
  override fun render(request: MapImageRequest): ByteArray = requireNotNull(
    javaClass.getResourceAsStream("/contextualreport/explanatory-map.jpg"),
  ).use { it.readBytes() }
}
