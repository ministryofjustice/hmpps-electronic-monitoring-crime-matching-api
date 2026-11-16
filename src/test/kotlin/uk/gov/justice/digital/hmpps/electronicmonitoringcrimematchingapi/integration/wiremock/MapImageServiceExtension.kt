package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class MapImageServiceExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val mapImageServiceApi = MapImageServiceMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    mapImageServiceApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    mapImageServiceApi.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    mapImageServiceApi.stop()
  }
}
