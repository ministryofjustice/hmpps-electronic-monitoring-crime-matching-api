package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.assertj.core.api.Assertions.assertThat
import org.docx4j.XmlUtils
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.wml.P
import org.docx4j.wml.R
import org.docx4j.wml.Text
import org.junit.jupiter.api.Test

class DocumentPageCountUpdaterTest {
  @Test
  fun `it should replace the page count placeholder`() {
    // Given
    val document = WordprocessingMLPackage.createPackage()

    document.mainDocumentPart.addParagraphOfText(
      "This statement consists of ${DocumentPageCountUpdater.PLACEHOLDER} pages.",
    )

    val updater = DocumentPageCountUpdater()

    // When
    updater.apply(document)

    // Then
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
