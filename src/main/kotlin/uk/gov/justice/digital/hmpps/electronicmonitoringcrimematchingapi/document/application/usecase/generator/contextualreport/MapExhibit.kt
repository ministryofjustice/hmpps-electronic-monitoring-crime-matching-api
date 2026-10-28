package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.jaxb.Context
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage

class MapExhibit(
  private val header: String,
  private val image: ByteArray,
  private val config: ContextualReportConfig,
) {
  private val factory = Context.getWmlObjectFactory()
  private val tableBuilder = ReportTableBuilder()
  private val paragraphBuilder = ReportParagraphBuilder()

  fun addTo(document: WordprocessingMLPackage) {
    val table = tableBuilder.create(width = config.contentWidth)
    val imagePart = BinaryPartAbstractImage.createImagePart(document, image)

    tableBuilder.addHeader(
      table = table,
      content = listOf(paragraphBuilder.create(header, bold = true)),
      columns = 1,
    )

    val inline = imagePart.createImageInline(
      header,
      header,
      1,
      2,
      false,
    )

    val drawing = org.docx4j.wml.ObjectFactory().createDrawing().apply {
      anchorOrInline.add(inline)
    }

    val run = factory.createR().apply {
      content.add(drawing)
    }

    val paragraph = paragraphBuilder.create("", centred = true)
    paragraph.content.add(run)

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
