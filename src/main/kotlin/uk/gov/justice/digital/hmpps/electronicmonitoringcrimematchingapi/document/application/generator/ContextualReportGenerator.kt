package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.generator

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.ContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentGenerator
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import java.io.ByteArrayOutputStream

class ContextualReportGenerator : DocumentGenerator {
  override val type = DocumentType.CONTEXTUAL_REPORT

  override fun generate(request: DocumentRequest): GeneratedDocument {
    val parameters = request.parameters as? ContextualReportRequestParameters
      ?: throw IllegalArgumentException(
        "Document request ${request.id} is not a contextual report",
      )

    val document = WordprocessingMLPackage.createPackage()

    document.mainDocumentPart.addParagraphOfText("Hello World")
    document.mainDocumentPart.addParagraphOfText("Contextual report parameters")
    document.mainDocumentPart.addParagraphOfText("Device ID: ${parameters.deviceId}")
    document.mainDocumentPart.addParagraphOfText("From: ${parameters.from}")
    document.mainDocumentPart.addParagraphOfText("To: ${parameters.to}")

    val output = ByteArrayOutputStream()
    document.save(output)

    return GeneratedDocument(
      filename = "contextual-report-${request.id}.docx",
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      content = output.toByteArray(),
    )
  }
}
