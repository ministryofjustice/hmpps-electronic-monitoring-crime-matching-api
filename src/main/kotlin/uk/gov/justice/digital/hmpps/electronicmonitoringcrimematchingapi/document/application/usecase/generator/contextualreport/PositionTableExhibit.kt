package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class PositionTableExhibit(
  private val number: Int,
  private val description: String,
  private val positions: List<Position>,
) {
  private val tableBuilder = ReportTableBuilder()
  private val paragraphBuilder = ReportParagraphBuilder()
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC)

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
          paragraphBuilder.create("Table goes here", spacingAfter = 120),
        ),
      ),
    )

    document.mainDocumentPart.addObject(table)
  }
}
