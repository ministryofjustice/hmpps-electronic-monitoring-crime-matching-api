package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import org.apache.commons.mail.util.MimeMessageParser
import java.io.InputStream
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.MimeMessage

fun extractAttachment(emailFile: InputStream): InputStream {
  val session = Session.getDefaultInstance(Properties())
  val mimeMessage = MimeMessage(session, emailFile)

  val parser = MimeMessageParser(mimeMessage).parse()

  return parser.attachmentList
    .firstOrNull { it.name?.endsWith(".csv", ignoreCase = true) == true }
    ?.inputStream
    ?: throw NoSuchElementException("No CSV attachment found in email")
}