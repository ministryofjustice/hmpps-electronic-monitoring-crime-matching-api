package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters
import java.time.Instant

class ContextualReportHeader(
  private val createdAt: Instant,
  private val parameters: ContextualReportRequestParameters,
  private val config: ContextualReportConfig,
) {
  private val date = config.dateFormatter.format(createdAt)
  private val content = "Contextual Movement Report for Crime Reference Number - ${parameters.crime.reference}"

  fun addTo(
    document: WordprocessingMLPackage,
  ) {
    val paragraphBuilder = ReportParagraphBuilder()
    val tableBuilder = ReportTableBuilder()
    val table = tableBuilder.create(width = config.contentWidth)

    tableBuilder.addHeader(
      table = table,
      content = listOf(
        paragraphBuilder.create("Date:", bold = true).apply {
          content.addAll(paragraphBuilder.create(" $date").content)
        },
      ),
      columns = 1,
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(
          paragraphBuilder.create(
            content,
            bold = true,
            centred = true,
          ),
        ),
      ),
    )

    document.mainDocumentPart.addObject(table)
  }
}
