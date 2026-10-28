package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.*
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.SensitivityLabel
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters
import java.io.ByteArrayOutputStream
import org.docx4j.model.pagination.Paginate
import org.slf4j.LoggerFactory


@Component
class ContextualReportDocxGenerator(
  private val documentSensitivityLabeller: DocumentSensitivityLabeller,
) : DocumentGenerator {
  private val log = LoggerFactory.getLogger(this::class.java)
  override val type = DocumentType.CONTEXTUAL_REPORT

  private val config = ContextualReportConfig()

  override fun generate(documentRequest: DocumentRequest): GeneratedDocument {
    val document = WordprocessingMLPackage.createPackage()
    val parameters = documentRequest.parameters as ContextualReportRequestParameters

    DocumentTheme(font = "Calibri").apply(document)

    documentSensitivityLabeller.apply(document, SensitivityLabel.OFFICIAL_SENSITIVE)

    ContextualReportHeader(documentRequest.createdAt, parameters, config).addTo(
      document = document,
    )

    document.mainDocumentPart.addParagraphOfText("")

    ContextualReportSummary(config).addTo(document)

    document.mainDocumentPart.addParagraphOfText("")

    ContextualReportPersonDetails(parameters, config).addTo(document)

    document.mainDocumentPart.addParagraphOfText("")

    ContextualReportCrimeDetails(parameters, config).addTo(document)

    document.mainDocumentPart.addParagraphOfText("")

    ContextualReportRequestDetails(parameters, config).addTo(document)

    document.mainDocumentPart.addParagraphOfText("")

    ContextualReportReportPeriod(parameters, config).addTo(document)

    document.mainDocumentPart.addParagraphOfText("")

    ContextualReportFurtherInformation(parameters, config).addTo(document)

    document.mainDocumentPart.addParagraphOfText("")

    ContextualReportDisclaimer(config).addTo(document)

    document.mainDocumentPart.addParagraphOfText("")

    ContextualReportWitnessStatement(documentRequest.createdAt, parameters, config).addTo(document)

    document.mainDocumentPart.addParagraphOfText("")

    ContextualReportExhibits(parameters, config).addTo(document)

    DocumentPageCountUpdater().apply(document)

    val byteStream = ByteArrayOutputStream()
    document.save(byteStream)

    return GeneratedDocument(
      filename = "placeholder.docx",
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      content = byteStream.toByteArray(),
    )
  }
}
