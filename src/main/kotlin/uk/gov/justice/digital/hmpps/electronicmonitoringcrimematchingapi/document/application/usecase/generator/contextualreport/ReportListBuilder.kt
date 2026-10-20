package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport

import org.docx4j.XmlUtils
import org.docx4j.jaxb.Context
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart
import org.docx4j.wml.Numbering
import org.docx4j.wml.P
import java.math.BigInteger

class ReportListBuilder {
  private val factory = Context.getWmlObjectFactory()

  fun createNumberedList(
    document: WordprocessingMLPackage,
    items: List<String>,
  ): List<P> = createList(
    document = document,
    items = items,
    format = "decimal",
    marker = "%1.",
  )

  fun createBulletedList(
    document: WordprocessingMLPackage,
    items: List<String>,
  ): List<P> = createList(
    document = document,
    items = items,
    format = "bullet",
    marker = "•",
  )

  private fun createList(
    document: WordprocessingMLPackage,
    items: List<String>,
    format: String,
    marker: String,
  ): List<P> {
    if (items.isEmpty()) return emptyList()

    val numberingPart = document.mainDocumentPart.numberingDefinitionsPart
      ?: NumberingDefinitionsPart().also {
        it.jaxbElement = factory.createNumbering()
        document.mainDocumentPart.addTargetPart(it)
      }

    val xml = """
      <w:abstractNum
          xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
          w:abstractNumId="0">
        <w:multiLevelType w:val="singleLevel"/>
        <w:lvl w:ilvl="0">
          <w:start w:val="1"/>
          <w:numFmt w:val="$format"/>
          <w:lvlText w:val="$marker"/>
          <w:lvlJc w:val="left"/>
          <w:pPr>
            <w:tabs>
              <w:tab w:val="num" w:pos="720"/>
            </w:tabs>
            <w:ind w:left="720" w:hanging="360"/>
          </w:pPr>
        </w:lvl>
      </w:abstractNum>
    """.trimIndent()

    val abstractNum = XmlUtils.unwrap(
      XmlUtils.unmarshalString(xml),
    ) as Numbering.AbstractNum

    // docx4j registers the definition and returns a concrete numbering instance.
    val numbering = numberingPart.addAbstractListNumberingDefinition(abstractNum)
    val numberingId = numbering.numId

    return items.mapIndexed { index, text ->
      factory.createP().apply {
        pPr = factory.createPPr().apply {
          numPr = factory.createPPrBaseNumPr().apply {
            ilvl = factory.createPPrBaseNumPrIlvl().apply {
              `val` = BigInteger.ZERO
            }

            numId = factory.createPPrBaseNumPrNumId().apply {
              `val` = numberingId
            }
          }

          spacing = factory.createPPrBaseSpacing().apply {
            before = BigInteger.ZERO
            after = if (index == items.lastIndex) {
              120.toBigInteger()
            } else {
              BigInteger.ZERO
            }
          }
        }

        content.add(
          factory.createR().apply {
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
}
