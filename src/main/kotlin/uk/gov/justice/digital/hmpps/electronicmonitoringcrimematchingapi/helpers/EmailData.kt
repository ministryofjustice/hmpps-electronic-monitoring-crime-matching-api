package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import jakarta.activation.DataSource
import java.util.Date

class EmailData(
  val sender: String,
  val originalSender: String,
  val subject: String,
  val sentAt: Date,
  val attachment: DataSource,
)
