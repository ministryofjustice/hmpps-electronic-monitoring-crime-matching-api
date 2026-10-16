package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.assertj.core.api.Assertions.assertThat
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.SensitivityLabel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class DocumentSensitivityLabelerTest {
  private val clock = Clock.fixed(
    Instant.parse("2026-10-08T12:00:00Z"),
    ZoneOffset.UTC,
  )

  private val labeler = DocumentSensitivityLabeller(clock)

  private val label = SensitivityLabel.OFFICIAL_SENSITIVE
  private val prefix = "MSIP_Label_${label.labelId}_"

  @Test
  fun `it should apply the official sensitive label to a document`() {
    // Given a new document
    val document = WordprocessingMLPackage.createPackage()

    // When the label is applied
    labeler.apply(document, label)

    // And the document is saved and reopened
    val output = ByteArrayOutputStream()
    document.save(output)

    val reopened = WordprocessingMLPackage.load(
      ByteArrayInputStream(output.toByteArray()),
    )

    // Then the expected sensitivity label properties should be present
    val properties = reopened.docPropsCustomPart.jaxbElement.property
      .associate { it.name to it.lpwstr }

    assertThat(properties).containsEntry(
      "${prefix}Enabled",
      "true",
    )
    assertThat(properties).containsEntry(
      "${prefix}Name",
      "OFFICIAL - SENSITIVE",
    )
    assertThat(properties).containsEntry(
      "${prefix}SiteId",
      "c6874728-71e6-41fe-a9e1-2e8c36776ad8",
    )
    assertThat(properties).containsEntry(
      "${prefix}Method",
      "Privileged",
    )
    assertThat(properties).containsEntry(
      "${prefix}SetDate",
      "2026-10-08T12:00:00Z",
    )
    assertThat(properties).containsEntry(
      "${prefix}ContentBits",
      "3",
    )
    assertThat(properties).containsEntry(
      "${prefix}Tag",
      "50, 0, 1, 1",
    )

    val actionId = properties["${prefix}ActionId"]

    assertThat(actionId).isNotNull()
    assertThat(UUID.fromString(actionId)).isNotNull()

    assertThat(reopened.docPropsCustomPart.jaxbElement.property.map { it.pid }).doesNotHaveDuplicates()
  }
}