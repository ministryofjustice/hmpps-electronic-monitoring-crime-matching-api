package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportHeader
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportPersonDetails
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.SensitivityLabel
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters
import java.io.ByteArrayOutputStream

class ContextualReportDocxGenerator(
  private val documentSensitivityLabeller: DocumentSensitivityLabeller,
) : DocumentGenerator {
  override val type = DocumentType.CONTEXTUAL_REPORT
  override fun generate(documentRequest: DocumentRequest): GeneratedDocument {
    val document = WordprocessingMLPackage.createPackage()
    val parameters = documentRequest.parameters as ContextualReportRequestParameters

    documentSensitivityLabeller.apply(document, SensitivityLabel.OFFICIAL_SENSITIVE)

    ContextualReportHeader().addTo(
      document = document,
      createdAt = documentRequest.createdAt,
      crimeReference = parameters.crime.reference,
    )

    document.mainDocumentPart.addParagraphOfText("")

    ContextualReportSummary().addTo(document)

    document.mainDocumentPart.addParagraphOfText("")

    ContextualReportPersonDetails().addTo(document, parameters.deviceWearer)

    val byteStream = ByteArrayOutputStream()
    document.save(byteStream)

    return GeneratedDocument(
      filename = "placeholder",
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      content = byteStream.toByteArray(),
    )
  }
}
