package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportConfig
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportCrimeDetails
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportDisclaimer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportExhibits
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportFurtherInformation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportHeader
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportPersonDetails
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportReportPeriod
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportRequestDetails
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportWitnessStatement
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.SensitivityLabel
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters
import java.io.ByteArrayOutputStream

@Component
class ContextualReportDocxGenerator(
  private val documentSensitivityLabeller: DocumentSensitivityLabeller,
) : DocumentGenerator {
  override val type = DocumentType.CONTEXTUAL_REPORT

  private val config = ContextualReportConfig()

  override fun generate(documentRequest: DocumentRequest): GeneratedDocument {
    val document = WordprocessingMLPackage.createPackage()
    val parameters = documentRequest.parameters as ContextualReportRequestParameters
    val exhibitTitles = createExhibitTitles(parameters, config)

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

    ContextualReportWitnessStatement(documentRequest.createdAt, parameters, config, exhibitTitles).addTo(document)

    document.mainDocumentPart.addParagraphOfText("")

    ContextualReportExhibits(parameters, config, exhibitTitles).addTo(document)

    DocumentPageCountUpdater().apply(document)

    val byteStream = ByteArrayOutputStream()
    document.save(byteStream)

    return GeneratedDocument(
      filename = "placeholder.docx",
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      content = byteStream.toByteArray(),
    )
  }

  private fun createExhibitTitles(parameters: ContextualReportRequestParameters, config: ContextualReportConfig): List<String> = buildList {
    add(
      createExhibitTitle(
        number = config.startingExhibitNumber,
        description = "Key for interpreting symbols on crime map",
      ),
    )

    add(
      createExhibitTitle(
        number = config.startingExhibitNumber + 1,
        description = "Key for interpreting location data table",
      ),
    )

    parameters.positionSegments.forEachIndexed { index, positions ->
      val mapExhibitNumber = config.startingExhibitNumber + 2 + index * 2
      val tableExhibitNumber = mapExhibitNumber + 1
      val from = config.dateTimeFormatterAlt.format(positions.first().gpsDate)
      val to = config.dateTimeFormatterAlt.format(positions.last().gpsDate)
      val dateRange = "$from and $to"

      add(
        createExhibitTitle(
          number = mapExhibitNumber,
          description = "Image(s) of the locations for ${parameters.deviceWearer.name}, between $dateRange",
        ),
      )

      add(
        createExhibitTitle(
          number = tableExhibitNumber,
          description = "Table of ${parameters.deviceWearer.name}'s locations, between $dateRange",
        ),
      )
    }
  }

  private fun createExhibitTitle(number: Int, description: String): String {
    val exhibitId = "EMAC/${number.toString().padStart(2, '0')}"

    return "Exhibit $exhibitId: $description"
  }
}
