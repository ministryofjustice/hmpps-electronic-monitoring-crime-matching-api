package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.wml.STVerticalJc
import org.docx4j.wml.Tbl
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.Position
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class PositionTableExhibit(
  private val number: Int,
  private val description: String,
  private val positions: List<Position>,
  private val includeDescriptions: Boolean = false,
) {
  private val tableBuilder = ReportTableBuilder()
  private val paragraphBuilder = ReportParagraphBuilder()
  private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneOffset.UTC)

  private val header: String
    get() {
      val exhibitId = "EMAC/${number.toString().padStart(2, '0')}"

      return "Exhibit $exhibitId: $description"
    }

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
          createPositionTable(),
          paragraphBuilder.create(""),
        ),
      ),
      cellPaddingVertical = 100L,
      cellPaddingHorizontal = 100L,
    )

    document.mainDocumentPart.addObject(table)
  }

  private fun createPositionTable(): Tbl {
    val table = tableBuilder.create()

    if (includeDescriptions) {
      tableBuilder.addRow(
        table = table,
        cells = columns.map { column ->
          listOf(
            paragraphBuilder.create(
              column["description"] ?: "",
              bold = true,
              centred = true,
              fontSize = 16, // 8pt
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
            fontSize = 16, // 8pt
            colour = "FFFFFF",
          ),
        )
      },
      backgroundColour = "306DBF",
      verticalAlign = STVerticalJc.TOP,
    )

    positions.forEach { position ->
      val values = listOf(
        dateFormatter.format(position.gpsDate),
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
              fontSize = 16, // 8pt
            ),
          )
        },
      )
    }

    return table
  }
}
