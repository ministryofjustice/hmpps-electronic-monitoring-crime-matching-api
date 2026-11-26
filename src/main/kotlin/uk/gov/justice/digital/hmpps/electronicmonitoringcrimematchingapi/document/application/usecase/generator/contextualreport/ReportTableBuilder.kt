package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.jaxb.Context
import org.docx4j.wml.CTBorder
import org.docx4j.wml.STBorder
import org.docx4j.wml.STShd
import org.docx4j.wml.STTblLayoutType
import org.docx4j.wml.STVerticalJc
import org.docx4j.wml.Tbl
import org.docx4j.wml.TblWidth
import org.docx4j.wml.Tc
import java.math.BigInteger

class ReportTableBuilder {
  private val factory = Context.getWmlObjectFactory()

  private val borderSize = 4L // 0.5pt
  private val defaultCellPaddingVertical = 40L
  private val defaultCellPaddingHorizontal = 120L

  fun create(columns: Int = 1, width: Long, columnWidths: List<Long>? = null): Tbl {
    require(columns > 0) {
      "Table must have at least one column"
    }
    require(columnWidths == null || (columnWidths.size == columns && columnWidths.all { it > 0 } && columnWidths.sum() == width)) {
      "Column widths must be positive, match the column count and sum to the table width"
    }

    return factory.createTbl().apply {
      tblPr = factory.createTblPr().apply {
        tblW = factory.createTblWidth().apply {
          type = "dxa"
          w = width.toBigInteger()
        }

        tblLayout = factory.createCTTblLayoutType().apply {
          type = STTblLayoutType.FIXED
        }

        tblBorders = factory.createTblBorders().apply {
          top = border()
          bottom = border()
          left = border()
          right = border()
          insideH = border()
          insideV = border()
        }
      }

      tblGrid = factory.createTblGrid().apply {
        (columnWidths ?: List(columns) { width / columns }).forEach { columnWidth ->
          gridCol.add(
            factory.createTblGridCol().apply {
              w = columnWidth.toBigInteger()
            },
          )
        }
      }
    }
  }

  fun addHeader(
    table: Tbl,
    content: List<Any>,
    columns: Int,
  ) {
    val row = factory.createTr()
    val tableWidth = table.tblPr.tblW.w.toLong()

    row.content.add(
      createCell(
        content = content,
        columnSpan = columns,
        backgroundColour = "D9D9D9",
        width = tableWidth,
      ),
    )

    table.content.add(row)
  }

  fun addRow(
    table: Tbl,
    cells: List<List<Any>>,
    columnSpan: Int = 1,
    backgroundColour: String? = null,
    verticalAlign: STVerticalJc = STVerticalJc.CENTER,
    cellPaddingVertical: Long = defaultCellPaddingVertical,
    cellPaddingHorizontal: Long = defaultCellPaddingHorizontal,
  ) {
    val row = factory.createTr()
    val columnWidths = table.tblGrid.gridCol.map { it.w.toLong() }
    require(columnSpan > 0 && cells.size * columnSpan == columnWidths.size) {
      "Row cells must span all table columns"
    }

    cells.forEachIndexed { index, content ->
      row.content.add(
        createCell(
          content = content,
          columnSpan = columnSpan,
          backgroundColour = backgroundColour,
          verticalAlign = verticalAlign,
          cellPaddingVertical = cellPaddingVertical,
          cellPaddingHorizontal = cellPaddingHorizontal,
          width = columnWidths.subList(index * columnSpan, (index + 1) * columnSpan).sum(),
        ),
      )
    }

    table.content.add(row)
  }

  private fun createCell(
    content: List<Any>,
    columnSpan: Int,
    width: Long,
    backgroundColour: String? = null,
    verticalAlign: STVerticalJc = STVerticalJc.CENTER,
    cellPaddingVertical: Long = defaultCellPaddingVertical,
    cellPaddingHorizontal: Long = defaultCellPaddingHorizontal,
  ): Tc = factory.createTc().apply {
    tcPr = factory.createTcPr().apply {
      tcW = factory.createTblWidth().apply {
        type = "dxa"
        w = BigInteger.valueOf(width)
      }

      vAlign = factory.createCTVerticalJc().apply {
        `val` = verticalAlign
      }

      if (columnSpan > 1) {
        gridSpan = factory.createTcPrInnerGridSpan().apply {
          `val` = columnSpan.toBigInteger()
        }
      }

      backgroundColour?.let {
        shd = factory.createCTShd().apply {
          fill = backgroundColour
          `val` = STShd.CLEAR
        }
      }

      tcMar = factory.createTcMar().apply {
        top = width(cellPaddingVertical, "dxa")
        bottom = width(cellPaddingVertical, "dxa")
        left = width(cellPaddingHorizontal, "dxa")
        right = width(cellPaddingHorizontal, "dxa")
      }
    }

    this.content.addAll(content)

    // Word requires a paragraph in an otherwise empty cell.
    if (content.isEmpty()) {
      this.content.add(factory.createP())
    }
  }

  private fun border(): CTBorder = factory.createCTBorder().apply {
    `val` = STBorder.SINGLE
    color = "000000"
    sz = BigInteger.valueOf(borderSize)
  }

  private fun width(
    value: Long,
    type: String,
  ): TblWidth = factory.createTblWidth().apply {
    this.type = type
    w = BigInteger.valueOf(value)
  }
}
