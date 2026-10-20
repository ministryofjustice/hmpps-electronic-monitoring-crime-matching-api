package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ContextualReportHeader {
  private val dateFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy")
    .withZone(ZoneOffset.UTC)

  private val headerPrefix = "Date:"
  private val contextPrefix = "Contextual Movement Report for Crime Reference Number -"

  fun addTo(
    document: WordprocessingMLPackage,
    createdAt: Instant,
    crimeReference: String,
  ) {
    val paragraphBuilder = ReportParagraphBuilder()
    val tableBuilder = ReportTableBuilder()
    val table = tableBuilder.create()

    tableBuilder.addHeader(
      table = table,
      content = listOf(
        paragraphBuilder.create("$headerPrefix ${dateFormatter.format(createdAt)}", bold = true),
      ),
      columns = 1,
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(
          paragraphBuilder.create(
            "$contextPrefix $crimeReference",
            bold = true,
            centred = true,
          ),
        ),
      ),
    )

    document.mainDocumentPart.addObject(table)
  }
}
