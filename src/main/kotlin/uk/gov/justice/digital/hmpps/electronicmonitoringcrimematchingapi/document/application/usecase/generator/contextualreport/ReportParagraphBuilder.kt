package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.jaxb.Context
import org.docx4j.wml.BooleanDefaultTrue
import org.docx4j.wml.HpsMeasure
import org.docx4j.wml.JcEnumeration
import org.docx4j.wml.P

class ReportParagraphBuilder {
  private val factory = Context.getWmlObjectFactory()

  fun create(
    text: String,
    bold: Boolean = false,
    centred: Boolean = false,
    fontSize: Int? = null,
    colour: String? = null,
    spacingAfter: Int = 0,
  ): P {
    // P = Paragraph
    // PPr = Paragraph Properties
    // Jc = Justification
    // R = Run (of text)
    // RPr = Run Properties
    return factory.createP().apply {
      pPr = factory.createPPr().apply {
        jc = factory.createJc().apply {
          `val` = if (centred) JcEnumeration.CENTER else JcEnumeration.LEFT
        }

        spacing = factory.createPPrBaseSpacing().apply {
          before = 0.toBigInteger()
          after = spacingAfter.toBigInteger()
        }
      }

      content.add(
        factory.createR().apply {
          rPr = factory.createRPr().apply {
            if (bold) {
              b = BooleanDefaultTrue()
            }
            fontSize?.let {
              sz = HpsMeasure().apply {
                `val` = fontSize.toBigInteger()
              }
              szCs = HpsMeasure().apply {
                `val` = fontSize.toBigInteger()
              }
            }
            colour?.let {
              color = factory.createColor().apply {
                `val` = it
              }
            }
          }

          content.add(
            factory.createText().apply {
              value = text
            },
          )
        },
      )
    }
  }
}
