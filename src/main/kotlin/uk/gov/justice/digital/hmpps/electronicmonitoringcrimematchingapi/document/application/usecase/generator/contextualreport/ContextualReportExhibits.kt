package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position
import java.time.Instant

class ContextualReportExhibits(
  private val parameters: ContextualReportRequestParameters,
  private val config: ContextualReportConfig,
  private val exhibitTitles: List<String>,
) {
  fun addTo(document: WordprocessingMLPackage) {
    MapExhibit(
      header = exhibitTitles[0],
      image = loadExplanatoryMap(),
      config = config,
    ).addTo(document)

    document.mainDocumentPart.addParagraphOfText("")

    PositionTableExhibit(
      header = exhibitTitles[1],
      positions = explanatoryPositions,
      includeDescriptions = true,
      config = config,
    ).addTo(document)

    document.mainDocumentPart.addParagraphOfText("")

    parameters.positionSegments.forEachIndexed { index, positions ->
      val mapExhibitTitleIndex = 2 + index * 2
      val tableExhibitTitleIndex = mapExhibitTitleIndex + 1

      MapExhibit(
        header = exhibitTitles[mapExhibitTitleIndex],
        image = loadExplanatoryMap(), // TODO - Can't render a 0 byte array, so using the example image for now
        config = config,
      ).addTo(document)

      document.mainDocumentPart.addParagraphOfText("")

      PositionTableExhibit(
        header = exhibitTitles[tableExhibitTitleIndex],
        positions = positions,
        config = config,
      ).addTo(document)
    }
  }

  private fun loadExplanatoryMap(): ByteArray = requireNotNull(
    javaClass.getResourceAsStream("/contextualreport/explanatory-map.jpg"),
  ) {
    "Explanatory map image not found"
  }.use { it.readBytes() }

  private val explanatoryPositions = listOf(
    Position(
      gpsDate = Instant.parse("2021-01-01T00:00:00Z"),
      latitude = 50.123456,
      longitude = -0.123456,
      precision = 50,
      speed = 21,
      direction = 30,
      sequenceNumber = 1,
    ),
    Position(
      gpsDate = Instant.parse("2021-01-01T00:01:00Z"),
      latitude = 50.234567,
      longitude = -0.234567,
      precision = 75,
      speed = 35,
      direction = 35,
      sequenceNumber = 2,
    ),
  )
}
