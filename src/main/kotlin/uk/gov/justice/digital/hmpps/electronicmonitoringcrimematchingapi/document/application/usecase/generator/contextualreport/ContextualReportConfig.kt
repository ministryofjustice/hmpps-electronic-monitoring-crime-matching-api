package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ContextualReportConfig(
  val timezone: ZoneId = ZoneId.of("Europe/London"),
  val pageWidth: Long = 11906L,
  val horizontalMargin: Long = 1440L,
) {
  val contentWidth = pageWidth - (horizontalMargin * 2)

  val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")
      .withZone(timezone)

  val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss")
      .withZone(timezone)

  val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
      .withZone(timezone)

  val dateTimeFormatterAlt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
      .withZone(timezone)

  val dateTimeFormatterAlt2: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
      .withZone(timezone)

  val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    .withZone(timezone)
}
