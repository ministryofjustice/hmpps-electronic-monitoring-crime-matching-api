package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.docx

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.wml.ObjectFactory
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentContentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequestStore
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.port.inbound.DocumentGenerator
import java.io.ByteArrayOutputStream

class ContextualReportDocxGenerator(
  private val documentStore: DocumentRequestStore,
) : DocumentGenerator {
  override fun generate(documentRequest: DocumentRequest) {
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

    documentRequest.document = byteStream.toByteArray()
    documentRequest.documentContentType = DocumentContentType.DOCX
    documentStore.save(documentRequest)
  }
}