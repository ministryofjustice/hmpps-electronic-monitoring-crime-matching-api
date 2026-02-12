package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import jakarta.activation.DataSource
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeUtility
import jakarta.validation.ValidationException
import java.io.InputStream
import java.util.Properties

fun extractEmailData(emailFile: InputStream): EmailData {
  val session = Session.getDefaultInstance(Properties())
  val mimeMessage = MimeMessage(session, emailFile)

  val subject = mimeMessage.subject
  val sender = mimeMessage.from
    ?.firstOrNull()
    ?.let { (it as? InternetAddress)?.address ?: it.toString() } ?: ""

  val originalSender = mimeMessage.getHeader("Resent-From", ", ") ?: sender

  val sentAt = mimeMessage.sentDate

  if (!subject.equals("Crime Mapping Request", ignoreCase = true)) throw ValidationException("Invalid email subject")

  val csvAttachments = mutableListOf<DataSource>()
  if (mimeMessage.isMimeType("multipart/*")) {
    val multipart = mimeMessage.content as Multipart
    for (i in 0 until multipart.count) {
      collectCsvAttachments(multipart.getBodyPart(i), csvAttachments)
    }
  }

  val attachment = when {
    csvAttachments.isEmpty() -> throw IllegalStateException("No CSV attachment found in email")
    csvAttachments.size > 1 -> throw NoSuchElementException("Multiple CSV attachments found")
    else -> csvAttachments.single()
  }

  return EmailData(
    sender = sender,
    originalSender = originalSender,
    subject = subject,
    sentAt = sentAt,
    attachment = attachment,
  )
}

private fun collectCsvAttachments(part: Part, out: MutableList<DataSource>) {
  if (part.isMimeType("multipart/*")) {
    val mp = part.content as Multipart
    for (i in 0 until mp.count) collectCsvAttachments(mp.getBodyPart(i), out)
    return
  }

  // Decode filename (can be RFC 2047 encoded)
  val rawName = part.fileName
  val fileName = rawName?.let { runCatching { MimeUtility.decodeText(it) }.getOrDefault(it) }

  val isAttachment =
    Part.ATTACHMENT.equals(part.disposition, ignoreCase = true) ||
      !fileName.isNullOrBlank() // treat named parts as attachments

  if (isAttachment && fileName?.endsWith(".csv", ignoreCase = true) == true) {
    out += part.dataHandler.dataSource
  }
}
