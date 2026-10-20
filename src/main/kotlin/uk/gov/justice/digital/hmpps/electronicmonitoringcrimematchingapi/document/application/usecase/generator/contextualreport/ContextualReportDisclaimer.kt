package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.jaxb.Context
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.wml.P

class ContextualReportDisclaimer {
  private val paragraphBuilder = ReportParagraphBuilder()
  private val tableBuilder = ReportTableBuilder()
  private val factory = Context.getWmlObjectFactory()

  private val header = "Disclaimer:"

  fun addTo(document: WordprocessingMLPackage) {
    val table = tableBuilder.create()

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
          ),
          paragraphBuilder.create(
            "In addition, it does not rule out other EM tag wearers being in the vicinity of the crime during the window of opportunity.",
          ),
          paragraphBuilder.create(
            "The results of this process are also limited to persons tagged as part of the MoJ’s Acquisitive Crime Project and only where there has been an ability to monitor the equipment during the window of opportunity of the crime.",
          ),
          paragraphBuilder.create(
            "Examples of an inability to monitor a person include:",
          ),
          bullet("Where the person has failed to charge the EM tag"),
          bullet("Where the tag has been removed from the person but is still able to transmit its location"),
          bullet(
            "Where the equipment has failed due to a technical fault. The technology used (radio frequency transmissions; GPS location monitoring and Location Based Services) has been proven to be reliable over many years. The monitoring equipment is extremely robust and reliable and meets the stringent specifications laid down by the Ministry of Justice. The monitoring equipment is also rigorously tested and audited by EMS, an independent body and the Home Office Scientific Branch.",
          ),
        ),
      ),
    )

    document.mainDocumentPart.addObject(table)
  }

  private fun bullet(text: String): P = paragraphBuilder.create("• $text").apply {
    pPr.ind = factory.createPPrBaseInd().apply {
      left = 360.toBigInteger()
      hanging = 180.toBigInteger()
    }
  }
}
