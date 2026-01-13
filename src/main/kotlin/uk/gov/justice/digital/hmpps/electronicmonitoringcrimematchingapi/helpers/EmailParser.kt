package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import org.apache.commons.mail.util.MimeMessageParser
import java.io.InputStream
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.MimeMessage

fun extractEmailData(emailFile: InputStream): EmailData {
  val session = Session.getDefaultInstance(Properties())
  val mimeMessage = MimeMessage(session, emailFile)

  val parser = MimeMessageParser(mimeMessage).parse()

  val subject = parser.subject
  val sender = parser.from
  // Placeholder until we're receiving forwarded emails
  val originalSender = parser.mimeMessage.getHeader("Resent-From", ", ") ?: sender
  val sentAt = parser.mimeMessage.sentDate

  val attachment = parser.attachmentList
    .firstOrNull { it.name?.endsWith(".csv", ignoreCase = true) == true }
    ?: throw NoSuchElementException("No CSV attachment found in email")

  return EmailData(
    sender,
    originalSender,
    subject,
    sentAt,
    attachment,
  )
}
