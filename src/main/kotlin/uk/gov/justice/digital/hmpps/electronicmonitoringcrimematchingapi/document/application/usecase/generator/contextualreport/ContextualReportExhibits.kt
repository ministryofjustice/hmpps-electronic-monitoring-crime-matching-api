package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ContextualReportExhibits(
  private val parameters: ContextualReportRequestParameters,
) {
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC)

  fun addTo(document: WordprocessingMLPackage) {
    val startingExhibitNumber = 6

    MapExhibit(
      number = 4,
      description = "Key for interpreting symbols on crime map",
      image = loadExplanatoryMap(),
    ).addTo(document)

    document.mainDocumentPart.addParagraphOfText("")

    PositionTableExhibit(
      number = 5,
      description = "Key for interpreting location data table",
      positions = listOf(),
    ).addTo(document)

    document.mainDocumentPart.addParagraphOfText("")

    parameters.positionSegments.forEachIndexed { index, positions ->
      val mapExhibitNumber = startingExhibitNumber + index * 2
      val tableExhibitNumber = mapExhibitNumber + 1
      val firstPosition = positions.first()
      val lastPosition = positions.last()
      val from = firstPosition.gpsDate
      val to = lastPosition.gpsDate
      val dateRange = "${dateFormatter.format(from)} and ${dateFormatter.format(to)}"

      MapExhibit(
        number = mapExhibitNumber,
        description = "Image(s) of the locations for ${parameters.deviceWearer.name}, between $dateRange",
        image = loadExplanatoryMap(), // TODO - Can't render a 0 byte array, so using the example image for now
      ).addTo(document)

      document.mainDocumentPart.addParagraphOfText("")

      PositionTableExhibit(
        number = tableExhibitNumber,
        description = "Table of ${parameters.deviceWearer.name}'s locations, between $dateRange",
        positions = positions,
      ).addTo(document)
    }
  }

  private fun loadExplanatoryMap(): ByteArray = requireNotNull(
    javaClass.getResourceAsStream("/contextualreport/explanatory-map.jpg"),
  ) {
    "Explanatory map image not found"
  }.use { it.readBytes() }
}
