import org.docx4j.jaxb.Context
import org.docx4j.wml.BooleanDefaultTrue
import org.docx4j.wml.JcEnumeration
import org.docx4j.wml.P

class ReportParagraphBuilder {
  private val factory = Context.getWmlObjectFactory()

  fun create(
    text: String,
    bold: Boolean = false,
    centred: Boolean = false,
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
          after = 0.toBigInteger()
        }
      }

      content.add(
        factory.createR().apply {
          if (bold) {
            rPr = factory.createRPr().apply {
              b = BooleanDefaultTrue()
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
