package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage

class ContextualReportSummary {
  fun addTo(document: WordprocessingMLPackage) {
    document.mainDocumentPart.addObject(
      ReportTableBuilder().create(
        heading = "Summary",
        content = """
          This report follows on from an EMAC Proximity Alert for the above crime and provides proportionate and
          necessary information in support of that investigation.
         """.trimIndent(),
      ),
    )
  }
}
