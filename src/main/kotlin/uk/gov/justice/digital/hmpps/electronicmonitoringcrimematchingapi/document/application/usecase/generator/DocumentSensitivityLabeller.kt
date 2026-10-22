package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.docx4j.docProps.custom.ObjectFactory
import org.docx4j.docProps.custom.Properties.Property
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.openpackaging.parts.DocPropsCustomPart
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
  private val siteId = "c6874728-71e6-41fe-a9e1-2e8c36776ad8"
  private val propertyFormatId =
    "{D5CDD505-2E9C-101B-9397-08002B2CF9AE}"

  fun apply(document: WordprocessingMLPackage, label: SensitivityLabel) {
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
}
