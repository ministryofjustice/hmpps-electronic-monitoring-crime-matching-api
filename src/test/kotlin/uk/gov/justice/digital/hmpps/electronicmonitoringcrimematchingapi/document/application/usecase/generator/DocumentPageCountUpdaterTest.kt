package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.assertj.core.api.Assertions.assertThat
import org.docx4j.XmlUtils
import org.docx4j.fonts.PhysicalFonts
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.wml.P
import org.docx4j.wml.R
import org.docx4j.wml.Text
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DocumentPageCountUpdaterTest {
  @ParameterizedTest
  @ValueSource(
    strings = [
      "file:/Library/Fonts/Calibri.ttf",
      "file:/C:/Windows/Fonts/calibrib.ttf",
      "file:/C:/Windows/Fonts/calibrili.ttf",
      "file:/System/Library/Fonts/Supplemental/Arial.ttf",
      "file:/System/Library/Fonts/Supplemental/Arial%20Bold%20Italic.ttf",
      "file:/C:/Windows/Fonts/arialbd.ttf",
      "file:/usr/share/fonts/truetype/carlito/Carlito-BoldItalic.ttf",
      "file:/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
      "file:/usr/share/fonts/truetype/dejavu/DejaVuSans-BoldOblique.ttf",
    ],
  )
  fun `it should discover report fonts and their variants`(fontUri: String) {
    DocumentPageCountUpdater()

    assertThat(Regex(PhysicalFonts.getRegex()).matches(fontUri)).isTrue()
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "file:/System/Library/Fonts/Supplemental/Arial%20Unicode.ttf",
      "file:/System/Library/Fonts/Supplemental/Arial%20Narrow.ttf",
      "file:/System/Library/Fonts/Apple%20Color%20Emoji.ttc",
      "file:/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
      "file:/Library/Fonts/Calibri/Unrelated.ttf",
    ],
  )
  fun `it should exclude unrelated and large system fonts`(fontUri: String) {
    DocumentPageCountUpdater()

    assertThat(Regex(PhysicalFonts.getRegex()).matches(fontUri)).isFalse()
  }

  @Test
  fun `it should replace the page count placeholder`() {
    // Given a document with a single paragraph containing the page count placeholder
    val document = WordprocessingMLPackage.createPackage()

    document.mainDocumentPart.addParagraphOfText(
      "This statement consists of ${DocumentPageCountUpdater.PLACEHOLDER} pages.",
    )

    // When the updater is applied
    DocumentPageCountUpdater().apply(document)

    // Then the placeholder should be replaced with the actual page count
    val text = document.mainDocumentPart.content
      .filterIsInstance<P>()
      .flatMap { it.content }
      .map(XmlUtils::unwrap)
      .filterIsInstance<R>()
      .flatMap { it.content }
      .map(XmlUtils::unwrap)
      .filterIsInstance<Text>()
      .joinToString("") { it.value }

    assertThat(text).doesNotContain(DocumentPageCountUpdater.PLACEHOLDER)
    assertThat(text).containsPattern(
      "This statement consists of 1 pages.",
    )
  }
}
