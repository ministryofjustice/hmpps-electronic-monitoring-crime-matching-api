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

  private val tableWidth = 5000 // 100% in fiftieths of a percent
  private val borderSize = 4L // 0.5pt
  private val cellPaddingVertical = 40L
  private val cellPaddingHorizontal = 120L

  fun create(columns: Int = 1): Tbl {
    require(columns > 0) {
      "Table must have at least one column"
    }

    return factory.createTbl().apply {
      tblPr = factory.createTblPr().apply {
        tblW = width(tableWidth.toLong(), "pct")

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
        repeat(columns) {
          gridCol.add(factory.createTblGridCol())
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

    row.content.add(
      createCell(
        content = content,
        shaded = true,
        columnSpan = columns,
      ),
    )

    table.content.add(row)
  }

  fun addRow(
    table: Tbl,
    cells: List<List<Any>>,
    columnSpan: Int = 1,
  ) {
    val row = factory.createTr()

    cells.forEach { content ->
      row.content.add(
        createCell(
          content = content,
          shaded = false,
          columnSpan = columnSpan,
        ),
      )
    }

    table.content.add(row)
  }

  private fun createCell(
    content: List<Any>,
    shaded: Boolean,
    columnSpan: Int,
  ): Tc = factory.createTc().apply {
    tcPr = factory.createTcPr().apply {
      vAlign = factory.createCTVerticalJc().apply {
        `val` = STVerticalJc.CENTER
      }

      if (columnSpan > 1) {
        gridSpan = factory.createTcPrInnerGridSpan().apply {
          `val` = columnSpan.toBigInteger()
        }
      }

      if (shaded) {
        shd = factory.createCTShd().apply {
          fill = "E6E6E6"
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
