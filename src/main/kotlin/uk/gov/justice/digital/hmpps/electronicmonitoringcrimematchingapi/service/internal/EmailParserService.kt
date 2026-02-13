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
    val message = MimeMessage(session, emailFile)

    val subject = message.subject
    val sender = (message.from?.firstOrNull() as? InternetAddress)?.address!!
    val sentAt = message.sentDate
    val redirectAddress = message.getHeader("Resent-From", null) ?: throw ValidationException("No redirect email")

    validateMetadata(subject, sender, redirectAddress)
    val attachment = extractCsvAttachment(message)

    return EmailData(
      sender,
      redirectAddress,
      subject,
      sentAt,
      attachment,
    )
  }

  private fun validateMetadata(subject: String, sender: String, redirectAddress: String) {
    if (!subject.equals("Crime Mapping Request", ignoreCase = true)) throw ValidationException("Invalid email subject")

    if (!redirectAddress.contains(properties.mailboxAddress, ignoreCase = true)) throw ValidationException("Invalid redirect email")

    if (!properties.validEmails.values.contains(sender.lowercase())) throw ValidationException("Invalid sender email")
  }

  private fun extractCsvAttachment(message: MimeMessage): DataSource {
    // Type has to be multipart for attachments
    val multipart = message.content as? Multipart ?: throw NoSuchElementException("No CSV attachment found in email")

    // Parse multipart for valid csv attachments
    val csvParts = (0 until multipart.count)
      .map(multipart::getBodyPart)
      .filter { part ->
        Part.ATTACHMENT.equals(part.disposition, ignoreCase = true) &&
          part.fileName?.endsWith(".csv", ignoreCase = true) == true
      }

    val part = when {
      csvParts.isEmpty() -> throw NoSuchElementException("No CSV attachment found in email")
      csvParts.size > 1 -> throw ValidationException("Multiple CSV attachments found")
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
