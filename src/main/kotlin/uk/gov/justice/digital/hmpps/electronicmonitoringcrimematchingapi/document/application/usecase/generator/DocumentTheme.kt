package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.docx4j.jaxb.Context
import org.docx4j.openpackaging.packages.WordprocessingMLPackage

class DocumentTheme(
  private val font: String,
  private val fontSize: Int = 21, // 10.5pt
) {
  fun apply(document: WordprocessingMLPackage) {
    val factory = Context.getWmlObjectFactory()
    val styles = document.mainDocumentPart.styleDefinitionsPart
    val normalStyle = styles.getStyleById("Normal")

    val rPr = normalStyle.rPr ?: factory.createRPr().also {
      normalStyle.rPr = it
    }

    rPr.rFonts = factory.createRFonts().apply {
      ascii = font
      hAnsi = font
      eastAsia = font
      cs = font
    }

    rPr.sz = factory.createHpsMeasure().apply {
      `val` = fontSize.toBigInteger()
    }

    rPr.szCs = factory.createHpsMeasure().apply {
      `val` = fontSize.toBigInteger()
    }
  }
}
