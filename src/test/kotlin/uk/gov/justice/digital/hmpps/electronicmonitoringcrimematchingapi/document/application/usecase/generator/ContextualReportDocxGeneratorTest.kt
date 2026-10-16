package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.assertj.core.api.Assertions.assertThat
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.SensitivityLabel
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Instant
import java.util.zip.ZipInputStream

class ContextualReportDocxGeneratorTest {
  @Test
  fun `it should generate a contextual report and return as a byte array`() {
    val generator = ContextualReportDocxGenerator(
      DocumentSensitivityLabeller(clock = Clock.systemUTC()),
    )
    // Given a document request for a contextual report
    val request = DocumentRequest.create(
      parameters = givenContextualReportRequestParameters(),
      createdBy = "user123",
      createdAt = Instant.parse("2026-10-01T08:05:00Z"),
    )

    // When the document is generated
    val generatedDocument = generator.generate(request)
    val bytes = generatedDocument.content

    // Then the content should not be empty
    assertThat(generator.type).isEqualTo(DocumentType.CONTEXTUAL_REPORT)
    assertThat(bytes).isNotEmpty

    val documentXml = ZipInputStream(ByteArrayInputStream(bytes)).use { zipInputStream ->
      generateSequence { zipInputStream.nextEntry }
        .firstOrNull { it.name == "word/document.xml" }
      zipInputStream.readBytes().toString(Charsets.UTF_8)
    }

    // And the document should contain the expected text
    assertThat(documentXml).contains("Hello world")

    val document = WordprocessingMLPackage.load(
      ByteArrayInputStream(generatedDocument.content),
    )

    val properties = document.docPropsCustomPart.jaxbElement.property

    // And the document should be labelled 'OFFICIAL - SENSITIVE'
    assertThat(properties)
      .anySatisfy {
        assertThat(it.name).isEqualTo(
          "MSIP_Label_${SensitivityLabel.OFFICIAL_SENSITIVE.labelId}_Name",
        )
        assertThat(it.lpwstr).isEqualTo(SensitivityLabel.OFFICIAL_SENSITIVE.displayName)
      }
  }
}
