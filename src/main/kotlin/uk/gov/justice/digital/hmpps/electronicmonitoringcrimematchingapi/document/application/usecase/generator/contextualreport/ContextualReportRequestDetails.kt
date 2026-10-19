package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ReportRequestDetails
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ContextualReportRequestDetails {
  private val dateFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy")
    .withZone(ZoneOffset.UTC)

  private val timeFormatter = DateTimeFormatter
    .ofPattern("HH:mm:ss")
    .withZone(ZoneOffset.UTC)

  private val header = "Requested by:"
  private val constabulary = "Constabulary"
  private val from = "From"
  private val date = "Date"
  private val time = "Time"

  fun addTo(
    document: org.docx4j.openpackaging.packages.WordprocessingMLPackage,
    request: ReportRequestDetails,
  ) {
    val paragraphBuilder = ReportParagraphBuilder()
    val tableBuilder = ReportTableBuilder()
    val table = tableBuilder.create(columns = 2)

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
        listOf(paragraphBuilder.create(constabulary)),
        listOf(paragraphBuilder.create(request.policeForceArea)),
      ),
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(paragraphBuilder.create(from)),
        listOf(paragraphBuilder.create(request.requestedBy)),
      ),
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(paragraphBuilder.create(date)),
        listOf(paragraphBuilder.create(dateFormatter.format(request.requestedAt))),
      ),
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(paragraphBuilder.create(time)),
        listOf(paragraphBuilder.create(timeFormatter.format(request.requestedAt))),
      ),
    )

    document.mainDocumentPart.addObject(table)
  }
}
