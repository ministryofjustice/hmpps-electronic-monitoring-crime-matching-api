package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.wml.ObjectFactory
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.SensitivityLabel
import java.io.ByteArrayOutputStream

class ContextualReportDocxGenerator(
  private val documentSensitivityLabeller: DocumentSensitivityLabeller,
) : DocumentGenerator {
  override val type = DocumentType.CONTEXTUAL_REPORT
  override fun generate(documentRequest: DocumentRequest): GeneratedDocument {
    val document = WordprocessingMLPackage.createPackage()
    val factory = ObjectFactory()

    documentSensitivityLabeller.apply(document, SensitivityLabel.OFFICIAL_SENSITIVE)

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
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      content = byteStream.toByteArray(),
    )
  }
}
