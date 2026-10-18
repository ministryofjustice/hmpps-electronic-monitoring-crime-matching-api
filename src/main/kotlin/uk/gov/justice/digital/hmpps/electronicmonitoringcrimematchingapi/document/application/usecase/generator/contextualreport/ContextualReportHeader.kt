package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ContextualReportHeader {
  private val dateFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy")
    .withZone(ZoneOffset.UTC)

  fun addTo(
    document: WordprocessingMLPackage,
    createdAt: Instant,
    crimeReference: String,
  ) {
    document.mainDocumentPart.addObject(
      ReportTableBuilder().create(
        heading = "Date: ${dateFormatter.format(createdAt)}",
        content = "Contextual Movement Report for Crime Reference Number - $crimeReference",
        contentCentred = true,
      ),
    )
  }
}
