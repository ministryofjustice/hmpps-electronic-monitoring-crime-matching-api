package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.zip.ZipInputStream

class ContextualReportDocxGeneratorTest {
  @Test
  fun `it should generate a contextual report and return as a byte array`() {
    val generator = ContextualReportDocxGenerator()
    val request = DocumentRequest.create(
      parameters = givenContextualReportRequestParameters(),
      createdBy = "user123",
      createdAt = Instant.parse("2026-10-01T08:05:00Z"),
    )

    val generatedDocument = generator.generate(request)
    val bytes = generatedDocument.content

    assertThat(generator.type).isEqualTo(DocumentType.CONTEXTUAL_REPORT)
    assertThat(bytes).isNotEmpty

    val documentXml = ZipInputStream(ByteArrayInputStream(bytes)).use { zipInputStream ->
      generateSequence { zipInputStream.nextEntry }
        .firstOrNull { it.name == "word/document.xml" }
      zipInputStream.readBytes().toString(Charsets.UTF_8)
    }

    assertThat(documentXml).contains("Hello world")
  }
}
