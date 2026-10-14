package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.wml.ObjectFactory
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.generator.DocumentGenerator
import java.io.ByteArrayOutputStream

class ContextualReportDocxGenerator : DocumentGenerator {
  override val type = DocumentType.CONTEXTUAL_REPORT
  override fun generate(documentRequest: DocumentRequest): GeneratedDocument {
    val document = WordprocessingMLPackage.createPackage()
    val factory = ObjectFactory()

    val paragraph = factory.createP()
    val run = factory.createR()
    val text = factory.createText().apply {
      value = "Hello world"
    }

    run.content.add(text)
    paragraph.content.add(run)
    document.mainDocumentPart.content.add(paragraph)
    val byteStream = ByteArrayOutputStream()
    document.save(byteStream)

    return GeneratedDocument(
      filename = "placeholder",
      contentType = document.contentType,
      content = byteStream.toByteArray(),
    )
  }
}
