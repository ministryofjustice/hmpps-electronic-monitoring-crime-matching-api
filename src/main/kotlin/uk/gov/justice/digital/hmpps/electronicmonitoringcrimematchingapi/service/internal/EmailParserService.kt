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
    val originalSender = (message.from?.firstOrNull() as? InternetAddress)?.address ?: throw ValidationException("Invalid sender email")
    val sentAt = message.sentDate
    val redirectHeader = message.getHeader("Resent-From", null) ?: throw ValidationException("No redirect email")
    val redirectAddress = InternetAddress.parse(redirectHeader).first().address

    validateMetadata(subject, redirectAddress)
    val attachments = extractCsvAttachment(message)

    return EmailData(
      sender = redirectAddress,
      originalSender = originalSender,
      subject = subject,
      sentAt = sentAt,
      attachments = attachments,
    )
  }

  private fun validateMetadata(subject: String, redirectAddress: String) {
    if (!subject.contains("Crime Mapping Request", ignoreCase = true)) throw ValidationException("Invalid email subject")

    if (!redirectAddress.equals(properties.mailboxAddress, ignoreCase = true)) throw ValidationException("Invalid redirect email")
  }

  private fun extractCsvAttachment(message: MimeMessage): List<DataSource> {
    val attachments = mutableListOf<DataSource>()
    // Type has to be multipart for attachments
    val multipart = message.content as? Multipart ?: return attachments

    // Parse multipart for valid csv attachments
    val csvParts = (0 until multipart.count)
      .map(multipart::getBodyPart)
      .filter { part ->
        Part.ATTACHMENT.equals(part.disposition, ignoreCase = true) &&
          part.fileName?.endsWith(".csv", ignoreCase = true) == true
      }

    for (part in csvParts) {
      val fileName = part.fileName
      val contentType = part.contentType
      val bytes = part.inputStream.use { it.readAllBytes() }

      val dataSource = ByteArrayDataSource(bytes, contentType).apply {
        name = fileName
      }
      attachments.add(
        dataSource,
      )
    }

    return attachments
  }
}
