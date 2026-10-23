package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage

class ContextualReportDisclaimer(
  private val config: ContextualReportConfig,
) {
  private val listBuilder = ReportListBuilder()
  private val paragraphBuilder = ReportParagraphBuilder()
  private val tableBuilder = ReportTableBuilder()

  private val header = "Disclaimer:"

  fun addTo(document: WordprocessingMLPackage) {
    val table = tableBuilder.create(width = config.contentWidth)

    tableBuilder.addHeader(
      table = table,
      content = listOf(paragraphBuilder.create(header, bold = true)),
      columns = 1,
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(
          paragraphBuilder.create(
            "The data disclosed in this report does not confirm that the tag wearer perpetrated or witnessed a crime.",
            spacingAfter = 120,
          ),
          paragraphBuilder.create(
            "In addition, it does not rule out other EM tag wearers being in the vicinity of the crime during the window of opportunity.",
            spacingAfter = 120,
          ),
          paragraphBuilder.create(
            "The results of this process are also limited to persons tagged as part of the MoJ’s Acquisitive Crime Project and only where there has been an ability to monitor the equipment during the window of opportunity of the crime.",
            spacingAfter = 120,
          ),
          paragraphBuilder.create(
            "Examples of an inability to monitor a person include:",
          ),
        ) +
          listBuilder.createBulletedList(
            document,
            listOf(
              "Where the person has failed to charge the EM tag",
              "Where the tag has been removed from the person but is still able to transmit its location",
              "Where the equipment has failed due to a technical fault. The technology used (radio frequency transmissions; GPS location monitoring and Location Based Services) has been proven to be reliable over many years. The monitoring equipment is extremely robust and reliable and meets the stringent specifications laid down by the Ministry of Justice. The monitoring equipment is also rigorously tested and audited by EMS, an independent body and the Home Office Scientific Branch.",
            ),
          ),
      ),

    )

    document.mainDocumentPart.addObject(table)
  }
}
