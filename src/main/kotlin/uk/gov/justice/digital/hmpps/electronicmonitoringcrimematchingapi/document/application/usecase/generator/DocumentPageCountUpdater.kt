package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.docx4j.XmlUtils
import org.docx4j.fonts.PhysicalFonts
import org.docx4j.model.pagination.Paginate
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.wml.ContentAccessor
import org.docx4j.wml.Text

class DocumentPageCountUpdater {
  companion object {
    const val PLACEHOLDER = "{{PAGE_COUNT}}"

    init {
      // Filter font-file URIs before the mapper loads them; avoid large, unrelated system fonts.
      PhysicalFonts.setRegex(
        "(?i).*/(?:calibri|carlito|arial|liberationsans|dejavusans)" +
          "(?:(?:[- ]|%20)*(?:regular|bolditalic|boldoblique|bold|italic|oblique|light|bd|bi|li|b|i|z|l))*\\.(?:ttf|otf)",
      )
    }
  }

  fun apply(document: WordprocessingMLPackage) {
    val pageCount = Paginate.compute(document, null).pageCount

    replacePlaceholder(
      document.mainDocumentPart.jaxbElement,
      pageCount.toString(),
    )
  }

  private fun replacePlaceholder(
    content: Any,
    replacement: String,
  ) {
    val element = XmlUtils.unwrap(content)

    if (element is Text) {
      element.value = element.value.replace(PLACEHOLDER, replacement)
    }

    if (element is ContentAccessor) {
      element.content.forEach {
        replacePlaceholder(it, replacement)
      }
    }
  }
}
