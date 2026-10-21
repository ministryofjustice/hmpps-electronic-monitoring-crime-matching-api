package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters

class ContextualReportFurtherInformation(
  private val parameters: ContextualReportRequestParameters,
) {
  private val header = "Further information (as supplied by Constabulary/Force):"

  fun addTo(document: WordprocessingMLPackage) {
    val paragraphBuilder = ReportParagraphBuilder()
    val tableBuilder = ReportTableBuilder()
    val table = tableBuilder.create()

    tableBuilder.addHeader(
      table = table,
      content = listOf(
        paragraphBuilder.create(header, bold = true),
      ),
      columns = 1,
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(
          paragraphBuilder.create(parameters.additionalInformation),
        ),
      ),
    )

    document.mainDocumentPart.addObject(table)
  }
}
