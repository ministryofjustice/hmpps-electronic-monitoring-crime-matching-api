package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage

class MapExhibit(
  private val number: Int,
  private val description: String,
  private val image: ByteArray,
) {
  private val tableBuilder = ReportTableBuilder()
  private val paragraphBuilder = ReportParagraphBuilder()

  private val header: String
    get() {
      val exhibitId = "EMAC/${number.toString().padStart(2, '0')}"

      return "Exhibit $exhibitId: $description"
    }

  fun addTo(document: WordprocessingMLPackage) {
    val table = tableBuilder.create()

    tableBuilder.addHeader(
      table = table,
      content = listOf(paragraphBuilder.create(header, bold = true)),
      columns = 1,
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(
          paragraphBuilder.create("Image goes here", spacingAfter = 120),
        ),
      ),
    )

    document.mainDocumentPart.addObject(table)
  }
}
