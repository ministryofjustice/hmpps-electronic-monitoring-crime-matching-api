package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage

class ContextualReportSummary {
  fun addTo(document: WordprocessingMLPackage) {
    document.mainDocumentPart.addObject(
      ReportTableBuilder().create(
        heading = "Summary",
        content = "Summary of the contextual movement report will be added here.",
      ),
    )
  }
}
