package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ContextualReportRequestParameters

class ContextualReportExhibitTitles(
  private val parameters: ContextualReportRequestParameters,
  private val config: ContextualReportConfig,
) {
  val titles: List<String> = buildList {
    add(
      createExhibitTitle(
        number = config.startingExhibitNumber,
        description = "Key for interpreting symbols on crime map",
      ),
    )

    add(
      createExhibitTitle(
        number = config.startingExhibitNumber + 1,
        description = "Key for interpreting location data table",
      ),
    )

    parameters.positionSegments.forEachIndexed { index, positions ->
      val mapExhibitNumber = config.startingExhibitNumber + 2 + index * 2
      val tableExhibitNumber = mapExhibitNumber + 1
      val from = config.dateTimeFormatterAlt.format(positions.first().gpsDate)
      val to = config.dateTimeFormatterAlt.format(positions.last().gpsDate)
      val dateRange = "$from and $to"

      add(
        createExhibitTitle(
          number = mapExhibitNumber,
          description = "Image(s) of the locations for ${parameters.deviceWearer.name}, between $dateRange",
        ),
      )

      add(
        createExhibitTitle(
          number = tableExhibitNumber,
          description = "Table of ${parameters.deviceWearer.name}'s locations, between $dateRange",
        ),
      )
    }
  }

  private fun createExhibitTitle(number: Int, description: String): String {
    val exhibitId = "EMAC/${number.toString().padStart(2, '0')}"

    return "Exhibit $exhibitId: $description"
  }
}
