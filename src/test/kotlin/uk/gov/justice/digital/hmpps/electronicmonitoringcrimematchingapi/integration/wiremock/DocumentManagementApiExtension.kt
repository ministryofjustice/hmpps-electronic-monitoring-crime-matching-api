package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.wiremock

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class DocumentManagementApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val documentManagementApi = DocumentManagementMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    documentManagementApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    documentManagementApi.resetAll()
    documentManagementApi.stubUploadDocument()
  }

  override fun afterAll(context: ExtensionContext) {
    documentManagementApi.stop()
  }
}
