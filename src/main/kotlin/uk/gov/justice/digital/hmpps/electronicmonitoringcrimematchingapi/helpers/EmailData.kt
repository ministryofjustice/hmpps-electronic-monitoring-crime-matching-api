package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import java.util.Date
import javax.activation.DataSource

class EmailData(
  val sender: String,
  val originalSender: String,
  val subject: String,
  val sentAt: Date,
  val attachment: DataSource,
)
