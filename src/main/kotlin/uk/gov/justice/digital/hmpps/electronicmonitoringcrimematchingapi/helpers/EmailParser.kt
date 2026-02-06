package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import jakarta.validation.ValidationException
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

  if (!parser.subject.equals("Crime Mapping Request", ignoreCase = true)) {
    throw ValidationException("Invalid email subject")
  }

  val attachment = parser.attachmentList
    .filter { it.name?.endsWith(".csv", ignoreCase = true) == true }
    .let { list ->
      when {
        list.isEmpty() -> throw NoSuchElementException("No CSV attachment found in email")
        list.size > 1 -> throw IllegalStateException("Multiple CSV attachments found")
        else -> list.single()
      }
    }

  return EmailData(
    sender,
    originalSender,
    subject,
    sentAt,
    attachment,
  )
}
