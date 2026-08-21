package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import jakarta.activation.DataSource
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * A minimal [DataSource] that only carries a file name. Used when rebuilding an
 * [uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome]
 * from a persisted outbox payload: the email-send path only reads the attachment name,
 * never its content.
 */
class NamedDataSource(private val fileName: String) : DataSource {
  override fun getInputStream(): InputStream = ByteArrayInputStream(ByteArray(0))

  override fun getOutputStream(): OutputStream = throw UnsupportedOperationException("NamedDataSource is read-only")

  override fun getContentType(): String = "application/octet-stream"

  override fun getName(): String = fileName
}
