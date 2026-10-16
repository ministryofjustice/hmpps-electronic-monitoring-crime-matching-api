package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.jaxb.Context
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.wml.BooleanDefaultTrue
import org.docx4j.wml.JcEnumeration
import org.docx4j.wml.STBorder
import org.docx4j.wml.STShd
import org.docx4j.wml.STTblLayoutType
import org.docx4j.wml.Tr
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ContextualReportHeader {
  private val factory = Context.getWmlObjectFactory()

  private val dateFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy")
    .withZone(ZoneOffset.UTC)

  fun addTo(
    document: WordprocessingMLPackage,
    createdAt: Instant,
    crimeReference: String,
  ) {
    val table = factory.createTbl().apply {
      tblPr = factory.createTblPr().apply {
        tblW = factory.createTblWidth().apply {
          type = "pct"
          w = 5000.toBigInteger()
        }

        tblLayout = factory.createCTTblLayoutType().apply {
          type = STTblLayoutType.AUTOFIT
        }

        tblBorders = factory.createTblBorders().apply {
          top = border()
          bottom = border()
          left = border()
          right = border()
          insideH = border()
        }
      }

      tblGrid = factory.createTblGrid().apply {
        gridCol.add(factory.createTblGridCol())
      }
    }

    table.content.add(
      row(
        "Date: ${dateFormatter.format(createdAt)}",
        shaded = true,
        centred = false,
      ),
    )

    table.content.add(
      row(
        "Contextual Movement Report for Crime Reference Number - $crimeReference",
        shaded = false,
        centred = true,
      ),
    )

    document.mainDocumentPart.addObject(table)
  }

  private fun row(
    value: String,
    shaded: Boolean,
    centred: Boolean,
  ): Tr = factory.createTr().apply {
    content.add(
      factory.createTc().apply {
        tcPr = factory.createTcPr().apply {
          if (shaded) {
            shd = factory.createCTShd().apply {
              fill = "E6E6E6"
              `val` = STShd.CLEAR
            }
          }

          tcMar = factory.createTcMar().apply {
            top = margin(40)
            bottom = margin(40)
            left = margin(120)
            right = margin(120)
          }
        }

        content.add(
          factory.createP().apply {
            pPr = factory.createPPr().apply {
              jc = factory.createJc().apply {
                `val` = if (centred) JcEnumeration.CENTER else JcEnumeration.LEFT
              }

              spacing = factory.createPPrBaseSpacing().apply {
                before = if (centred) 120.toBigInteger() else 0.toBigInteger()
                after = if (centred) 120.toBigInteger() else 0.toBigInteger()
              }
            }

            content.add(
              factory.createR().apply {
                rPr = factory.createRPr().apply {
                  b = BooleanDefaultTrue()
                }

                content.add(
                  factory.createText().apply {
                    this.value = value
                  },
                )
              },
            )
          },
        )
      },
    )
  }

  private fun border() = factory.createCTBorder().apply {
    `val` = STBorder.SINGLE
    color = "000000"
    sz = 8.toBigInteger()
  }

  private fun margin(value: Long) = factory.createTblWidth().apply {
    type = "dxa"
    w = value.toBigInteger()
  }
}
