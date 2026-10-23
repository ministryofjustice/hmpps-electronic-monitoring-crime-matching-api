package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport
import org.docx4j.openpackaging.packages.WordprocessingMLPackage

class ContextualReportSummary(
  private val config: ContextualReportConfig,
) {
  private val header = "Summary"
  private val content = """
    This report follows on from an EMAC Proximity Alert for the above crime and provides proportionate and necessary information in support of that investigation.
  """.trimIndent()

  fun addTo(document: WordprocessingMLPackage) {
    val paragraphBuilder = ReportParagraphBuilder()
    val tableBuilder = ReportTableBuilder()
    val table = tableBuilder.create(width = config.contentWidth)

    tableBuilder.addHeader(
      table = table,
      content = listOf(
        paragraphBuilder.create(header, bold = true),
      ),
      columns = 1,
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(
          paragraphBuilder.create(content),
        ),
      ),
    )

    document.mainDocumentPart.addObject(table)
  }
}
