package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import jakarta.activation.DataSource
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.util.ByteArrayDataSource
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.emailIngestion.EmailIngestionProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import java.io.InputStream
import java.util.Properties

@Service
class EmailParserService(
  private val properties: EmailIngestionProperties,
) {

  fun extractEmailData(emailFile: InputStream): EmailData {
    val session = Session.getDefaultInstance(Properties())
    val mimeMessage = MimeMessage(session, emailFile)

    val subject = mimeMessage.subject
    val sender = (mimeMessage.from?.firstOrNull() as? InternetAddress)?.address!!
    val sentAt = mimeMessage.sentDate!!
    val redirectAddress = mimeMessage.getHeader("Resent-From", null) ?: throw ValidationException("No redirect email")

    if (!subject.equals("Crime Mapping Request", ignoreCase = true)) throw ValidationException("Invalid email subject")

    if (!redirectAddress.contains(properties.mailboxAddress, ignoreCase = true)) throw ValidationException("Invalid redirect email")

    if (!properties.validEmails.values.contains(sender.lowercase())) throw ValidationException("Invalid sender email")

    val attachment = extractCsvAttachment(mimeMessage)

    return EmailData(
      sender,
      redirectAddress,
      subject,
      sentAt,
      attachment,
    )
  }

  private fun extractCsvAttachment(message: MimeMessage): DataSource {
    // Type has to be multipart for attachments
    if (!message.isMimeType("multipart/*")) throw ValidationException("No CSV attachment found in email")

    val multipart = message.content as Multipart

    // Parse multipart for valid csv attachments
    val csvParts = (0 until multipart.count)
      .map { multipart.getBodyPart(it) }
      .filter { part ->
        val fileName = part.fileName
        Part.ATTACHMENT.equals(part.disposition, ignoreCase = true) &&
          fileName?.endsWith(".csv", ignoreCase = true) == true
      }

    val part = when {
      csvParts.isEmpty() -> throw ValidationException("No CSV attachment found in email")
      csvParts.size > 1 -> throw NoSuchElementException("Multiple CSV attachments found")
      else -> csvParts.single()
    }

    // Construct datasource from valid csv part
    val fileName = part.fileName
    val contentType = part.contentType
    val bytes = part.inputStream.use { it.readAllBytes() }

    return ByteArrayDataSource(bytes, contentType).apply {
      name = fileName
    }
  }
}
