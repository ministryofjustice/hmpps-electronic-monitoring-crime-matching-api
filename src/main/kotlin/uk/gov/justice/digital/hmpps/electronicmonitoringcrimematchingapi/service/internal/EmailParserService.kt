package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import jakarta.validation.ValidationException
import org.apache.commons.mail.util.MimeMessageParser
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.emailIngestion.EmailIngestionProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import java.io.InputStream
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.MimeMessage

@Service
class EmailParserService(
  private val properties: EmailIngestionProperties,
) {

  fun extractEmailData(emailFile: InputStream): EmailData {
    val session = Session.getDefaultInstance(Properties())
    val mimeMessage = MimeMessage(session, emailFile)

    val parser = MimeMessageParser(mimeMessage).parse()

    val subject = parser.subject
    val sender = parser.from
    val redirectAddress = parser.mimeMessage.getHeader("Resent-From", null) ?: throw ValidationException("No redirect email")
    val sentAt = parser.mimeMessage.sentDate

    if (!parser.subject.equals("Crime Mapping Request", ignoreCase = true)) throw ValidationException("Invalid email subject")

    if (!redirectAddress.contains(properties.mailboxAddress, ignoreCase = true)) throw ValidationException("Invalid redirect email")

    if (!properties.validEmails.values.contains(sender.lowercase())) throw ValidationException("Invalid sender email")

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
      redirectAddress,
      subject,
      sentAt,
      attachment,
    )
  }
}
