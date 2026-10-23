package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.docx4j.docProps.custom.ObjectFactory
import org.docx4j.docProps.custom.Properties.Property
import org.docx4j.jaxb.Context
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.openpackaging.parts.DocPropsCustomPart
import org.docx4j.openpackaging.parts.WordprocessingML.FooterPart
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart
import org.docx4j.wml.HdrFtrRef
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.SensitivityLabel
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Component
class DocumentSensitivityLabeller(
  private val clock: Clock,
) {
  private val factory = Context.getWmlObjectFactory()
  private val siteId = "c6874728-71e6-41fe-a9e1-2e8c36776ad8"
  private val propertyFormatId =
    "{D5CDD505-2E9C-101B-9397-08002B2CF9AE}"

  fun apply(document: WordprocessingMLPackage, label: SensitivityLabel) {
    applySensitivityMetadata(document, label)
    applyVisibleMarkings(document, label.displayName)
  }

  private fun applySensitivityMetadata(document: WordprocessingMLPackage, label: SensitivityLabel) {
    val customProperties = DocPropsCustomPart()
    val properties = ObjectFactory().createProperties()
    val prefix = "MSIP_Label_${label.labelId}_"
    val values = linkedMapOf(
      "Enabled" to "true",
      "SetDate" to Instant.now(clock)
        .truncatedTo(ChronoUnit.SECONDS)
        .toString(),
      "Method" to "Privileged",
      "Name" to label.displayName,
      "SiteId" to siteId,
      "ActionId" to UUID.randomUUID().toString(),
    )

    values.entries.forEachIndexed { index, (name, value) ->
      properties.property.add(
        Property().apply {
          fmtid = propertyFormatId
          pid = index + 2
          this.name = prefix + name
          lpwstr = value
        },
      )
    }

    customProperties.jaxbElement = properties
    document.addTargetPart(customProperties)
  }

  private fun applyVisibleMarkings(document: WordprocessingMLPackage, label: String) {
    val mainPart = document.mainDocumentPart

    val sectPr = mainPart.jaxbElement.body.sectPr
      ?: factory.createSectPr().also {
        mainPart.jaxbElement.body.sectPr = it
      }

    val headerPart = HeaderPart().apply {
      jaxbElement = factory.createHdr().apply {
        content.add(classificationParagraph(label))
      }
    }

    val footerPart = FooterPart().apply {
      jaxbElement = factory.createFtr().apply {
        content.add(classificationParagraph(label))
      }
    }

    val headerRelationship = mainPart.addTargetPart(headerPart)
    val footerRelationship = mainPart.addTargetPart(footerPart)

    sectPr.egHdrFtrReferences.add(
      factory.createHeaderReference().apply {
        type = HdrFtrRef.DEFAULT
        id = headerRelationship.id
      },
    )

    sectPr.egHdrFtrReferences.add(
      factory.createFooterReference().apply {
        type = HdrFtrRef.DEFAULT
        id = footerRelationship.id
      },
    )
  }

  private fun classificationParagraph(label: String) = factory.createP().apply {
    pPr = factory.createPPr().apply {
      jc = factory.createJc().apply {
        `val` = org.docx4j.wml.JcEnumeration.CENTER
      }
    }

    content.add(
      factory.createR().apply {
        content.add(
          factory.createText().apply {
            value = label
          },
        )
      },
    )
  }
}
