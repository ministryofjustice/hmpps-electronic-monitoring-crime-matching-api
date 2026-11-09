package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class AlgorithmApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val algorithmApi = AlgorithmMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    algorithmApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    algorithmApi.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    algorithmApi.stop()
  }
}
