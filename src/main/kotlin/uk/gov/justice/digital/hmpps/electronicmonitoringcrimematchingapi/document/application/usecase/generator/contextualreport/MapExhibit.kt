package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage

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
    val imagePart = BinaryPartAbstractImage.createImagePart(document, image)

    tableBuilder.addHeader(
      table = table,
      content = listOf(paragraphBuilder.create(header, bold = true)),
      columns = 1,
    )

    val inline = imagePart.createImageInline(
      "Explanatory map",
      "Key for interpreting symbols on crime map",
      1,
      2,
      false,
    )

    val drawing = org.docx4j.wml.ObjectFactory().createDrawing().apply {
      anchorOrInline.add(inline)
    }

    val paragraph = paragraphBuilder.create("", centred = true)
    paragraph.content.add(drawing)

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(
          paragraph,
        ),
      ),
    )

    document.mainDocumentPart.addObject(table)
  }
}
