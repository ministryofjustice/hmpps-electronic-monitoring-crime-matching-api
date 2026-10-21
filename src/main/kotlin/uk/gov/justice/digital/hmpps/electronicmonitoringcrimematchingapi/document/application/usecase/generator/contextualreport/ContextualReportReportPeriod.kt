package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ContextualReportReportPeriod(
  private val parameters: ContextualReportRequestParameters,
) {
  private val dateFormatter = DateTimeFormatter
    .ofPattern("HH:mm dd/MM/yyyy")
    .withZone(ZoneOffset.UTC)

  private val header = "Request summary:"
  private val from = "Datetime CR requested from"
  private val to = "Datetime CR requested to"
  private val images = "Images"
  private val imagesContent = "Between Above Date/Time Range"

  fun addTo(
    document: WordprocessingMLPackage,
  ) {
    val paragraphBuilder = ReportParagraphBuilder()
    val tableBuilder = ReportTableBuilder()
    val table = tableBuilder.create(columns = 2)

    tableBuilder.addHeader(
      table = table,
      content = listOf(
        paragraphBuilder.create(header, bold = true),
      ),
      columns = 2,
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(paragraphBuilder.create(from)),
        listOf(paragraphBuilder.create(dateFormatter.format(parameters.reportPeriod.start))),
      ),
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(paragraphBuilder.create(to)),
        listOf(paragraphBuilder.create(dateFormatter.format(parameters.reportPeriod.end))),
      ),
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(paragraphBuilder.create(images)),
        listOf(paragraphBuilder.create(imagesContent)),
      ),
    )

    document.mainDocumentPart.addObject(table)
  }
}
