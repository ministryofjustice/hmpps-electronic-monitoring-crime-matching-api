package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters

class ContextualReportCrimeDetails(
  private val parameters: ContextualReportRequestParameters,
  private val config: ContextualReportConfig,
) {
  private val header = "In relation to:"
  private val batchId = "Crime Mapping Batch ID"
  private val crimeRef = "Crime Reference Number"
  private val crimeType = "Crime Type"

  fun addTo(
    document: WordprocessingMLPackage,
  ) {
    val paragraphBuilder = ReportParagraphBuilder()
    val tableBuilder = ReportTableBuilder()
    val table = tableBuilder.create(width = config.contentWidth, columns = 2)

    tableBuilder.addHeader(
      table = table,
      content = listOf(
        paragraphBuilder.create(header, bold = true),
      ),
      columns = 2,
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(paragraphBuilder.create(batchId)),
        listOf(paragraphBuilder.create(parameters.crime.batchId)),
      ),
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(paragraphBuilder.create(crimeRef)),
        listOf(paragraphBuilder.create(parameters.crime.reference)),
      ),
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(paragraphBuilder.create(crimeType)),
        listOf(paragraphBuilder.create(parameters.crime.type)),
      ),
    )

    document.mainDocumentPart.addObject(table)
  }
}
