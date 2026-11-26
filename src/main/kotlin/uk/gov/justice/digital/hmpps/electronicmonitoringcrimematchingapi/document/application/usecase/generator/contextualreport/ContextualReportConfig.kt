package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Configuration
class ContextualReportConfig(
  val timezone: ZoneId = ZoneId.of("Europe/London"),
  val pageWidth: Long = 11906L,
  val horizontalMargin: Long = 1440L,
  val startingExhibitNumber: Int = 4,
  val mapPaddingPx: Int = 80,
  val gnssGapThreshold: Duration = Duration.ofMinutes(1),
  val gnssGapTrackDashPattern: List<Int> = listOf(8, 8),
) {
  val contentWidth = pageWidth - (horizontalMargin * 2)

  val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy")
      .withZone(timezone)

  val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss")
      .withZone(timezone)

  val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
      .withZone(timezone)

  val dateTimeWithSecondsFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
      .withZone(timezone)
}
