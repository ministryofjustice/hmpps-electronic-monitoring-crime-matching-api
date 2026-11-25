package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.wml.STVerticalJc
import org.docx4j.wml.Tbl
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.GnssGap
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position

class PositionTableExhibit(
  private val header: String,
  private val positions: List<Position>,
  private val includeDescriptions: Boolean = false,
  private val config: ContextualReportConfig,
  private val gaps: Map<Int, GnssGap> = emptyMap(),
) {
  private val tableBuilder = ReportTableBuilder()
  private val paragraphBuilder = ReportParagraphBuilder()

  private val columns = listOf(
    mapOf(
      "heading" to "DATE/TIME",
      "description" to "Represents the date and time of recorded location",
    ),
    mapOf(
      "heading" to "LATITUDE",
      "description" to "Represents the latitude of recorded location",
    ),
    mapOf(
      "heading" to "LONGITUDE",
      "description" to "Represents the longitude of recorded location",
    ),
    mapOf(
      "heading" to "CONFIDENCE CIRCLE",
      "description" to "Represents the confidence circle of recorded location",
    ),
    mapOf(
      "heading" to "SPEED",
      "description" to "Represents the speed between consecutive recorded locations 0.1 Km/h (i.e., 21 = 2.1 km/h)",
    ),
    mapOf(
      "heading" to "DIRECTION",
      "description" to "Represents the direction in degrees between consecutive recorded locations",
    ),
    mapOf(
      "heading" to "SEQUENCE NO.",
      "description" to "Correlates to numbered location points on the corresponding map and the data in the table of that row",
    ),
  )

  private val nestedTablePadding = 100L

  fun addTo(document: WordprocessingMLPackage) {
    val table = tableBuilder.create(width = config.contentWidth)

    tableBuilder.addHeader(
      table = table,
      content = listOf(paragraphBuilder.create(header, fontSize = config.exhibitTableFontSize)),
      columns = 1,
    )

    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(
          createPositionTable(),
          paragraphBuilder.create("", fontSize = config.exhibitTableFontSize),
        ),
      ),
      cellPaddingVertical = nestedTablePadding,
      cellPaddingHorizontal = nestedTablePadding,
    )

    document.mainDocumentPart.addObject(table)
  }

  private fun createPositionTable(): Tbl {
    val table = tableBuilder.create(
      width = config.contentWidth - nestedTablePadding * 2,
      columns = columns.size,
    ) // Subtract padding from width

    if (includeDescriptions) {
      tableBuilder.addRow(
        table = table,
        cells = columns.map { column ->
          listOf(
            paragraphBuilder.create(
              column["description"] ?: "",
              bold = true,
              centred = true,
              fontSize = config.exhibitTableFontSize,
              colour = "FFFFFF",
            ),
          )
        },
        backgroundColour = "62B3F5",
        verticalAlign = STVerticalJc.TOP,
      )
    }

    tableBuilder.addRow(
      table = table,
      cells = columns.map { column ->
        listOf(
          paragraphBuilder.create(
            column["heading"] ?: "",
            bold = true,
            centred = true,
            fontSize = config.exhibitTableFontSize,
            colour = "FFFFFF",
          ),
        )
      },
      backgroundColour = "306DBF",
      verticalAlign = STVerticalJc.TOP,
    )

    positions.forEachIndexed { index, position ->
      gaps[index]?.let { addGapRow(table, it) }
      val values = listOf(
        config.dateTimeWithSecondsFormatter.format(position.gpsDate),
        position.latitude.toString(),
        position.longitude.toString(),
        position.precision.toString(),
        position.speed.toString(),
        position.direction.toString(),
        position.sequenceNumber.toString(),
      )

      tableBuilder.addRow(
        table = table,
        cells = values.map { value ->
          listOf(
            paragraphBuilder.create(
              value,
              fontSize = config.exhibitTableFontSize,
            ),
          )
        },
      )
    }
    gaps[positions.size]?.let { addGapRow(table, it) }

    return table
  }

  private fun addGapRow(table: Tbl, gap: GnssGap) {
    val from = config.dateTimeWithSecondsFormatter.format(gap.start)
    val to = config.dateTimeWithSecondsFormatter.format(gap.end)
    tableBuilder.addRow(
      table = table,
      cells = listOf(
        listOf(
          paragraphBuilder.create(
            "No GNSS data between $from and $to",
            centred = true,
            fontSize = config.exhibitTableFontSize,
          ),
        ),
      ),
      columnSpan = columns.size,
    )
  }
}
