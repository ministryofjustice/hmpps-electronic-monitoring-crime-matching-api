package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class NotifyApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val notifyMockServer = NotifyMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    notifyMockServer.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    notifyMockServer.resetAll()
    notifyMockServer.stubSendEmail()
  }

  override fun afterAll(context: ExtensionContext) {
    notifyMockServer.stop()
  }
}
