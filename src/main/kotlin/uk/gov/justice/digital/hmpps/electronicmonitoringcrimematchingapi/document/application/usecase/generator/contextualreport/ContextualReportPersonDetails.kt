package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import ReportParagraphBuilder
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.ReportTableBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.DeviceWearerDetails
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ContextualReportPersonDetails {
  private val dateFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy")
    .withZone(ZoneOffset.UTC)

  private val header: String = "Person:"

  fun addTo(
    document: WordprocessingMLPackage,
    deviceWearer: DeviceWearerDetails,
  ) {
    val paragraphBuilder = ReportParagraphBuilder()
    val tableBuilder = ReportTableBuilder()
    val table = tableBuilder.create()

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
        listOf(paragraphBuilder.create("Full Name")),
        listOf(paragraphBuilder.create(deviceWearer.name)),
      ),
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(paragraphBuilder.create("D.O.B.")),
        listOf(paragraphBuilder.create(deviceWearer.dateOfBirth.format(dateFormatter))),
      ),
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(paragraphBuilder.create("PNC Number")),
        listOf(paragraphBuilder.create(deviceWearer.pncNumber)),
      ),
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(paragraphBuilder.create("Specified Address")),
        listOf(paragraphBuilder.create(deviceWearer.address)),
      ),
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(paragraphBuilder.create("EMS ID")),
        listOf(paragraphBuilder.create(deviceWearer.emsId)),
      ),
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(paragraphBuilder.create("Offender Manager")),
        listOf(paragraphBuilder.create(deviceWearer.offenderManager)),
      ),
    )

    document.mainDocumentPart.addObject(table)
  }
}
