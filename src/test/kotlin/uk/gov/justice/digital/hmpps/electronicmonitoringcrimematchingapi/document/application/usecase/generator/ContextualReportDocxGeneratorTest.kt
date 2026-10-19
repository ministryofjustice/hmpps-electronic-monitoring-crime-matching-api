package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.assertj.core.api.Assertions.assertThat
import org.docx4j.XmlUtils
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.wml.P
import org.docx4j.wml.R
import org.docx4j.wml.Tbl
import org.docx4j.wml.Tc
import org.docx4j.wml.Text
import org.docx4j.wml.Tr
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.SensitivityLabel
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters
import java.io.ByteArrayInputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.zip.ZipInputStream

class ContextualReportDocxGeneratorTest {
  private val generator = ContextualReportDocxGenerator(
    DocumentSensitivityLabeller(
      clock = Clock.fixed(
        Instant.parse("2026-10-08T12:00:00Z"),
        ZoneOffset.UTC,
      ),
    ),
  )

  @Test
  fun `it should generate a contextual report and return as a byte array`() {
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
    assertThat(documentXml).contains("Contextual Movement Report")

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

  @Test
  fun `it should generate the contextual report header`() {
    // Given a document request for a contextual report
    val request = DocumentRequest.create(
      parameters = givenContextualReportRequestParameters(),
      createdBy = "user123",
      createdAt = Instant.parse("2026-10-01T08:05:00Z"),
    )

    // When the document is generated
    val result = generator.generate(request)

    val document = WordprocessingMLPackage.load(
      ByteArrayInputStream(result.content),
    )

    // Then the document should contain a table
    val table = document.mainDocumentPart.content
      .unwrapped<Tbl>()
      .first()

    val rows = table.content.unwrapped<Tr>()

    // And the table should have two rows
    assertThat(rows).hasSize(2)

    val titleCell = rows[0].content
      .unwrapped<Tc>()
      .single()

    val contentCell = rows[1].content
      .unwrapped<Tc>()
      .single()

    // And the first row should contain the title
    assertThat(titleCell.text()).contains("Date: 01/10/2026")

    // And the second row should contain the content
    assertThat(contentCell.text()).contains(
      "Contextual Movement Report for Crime Reference Number - 123456789",
    )

    // And the first row should have a grey background
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("E6E6E6")
  }

  @Test
  fun `it should generate the contextual report summary`() {
    // Given a document request for a contextual report
    val request = DocumentRequest.create(
      parameters = givenContextualReportRequestParameters(),
      createdBy = "user123",
      createdAt = Instant.parse("2026-10-01T08:05:00Z"),
    )

    // When the document is generated
    val result = generator.generate(request)

    val document = WordprocessingMLPackage.load(
      ByteArrayInputStream(result.content),
    )

    // Then the document should contain a second table
    val table = document.mainDocumentPart.content
      .unwrapped<Tbl>()
      .drop(1)
      .first()

    val rows = table.content.unwrapped<Tr>()

    // And the table should have two rows
    assertThat(rows).hasSize(2)

    val titleCell = rows[0].content
      .unwrapped<Tc>()
      .single()

    val contentCell = rows[1].content
      .unwrapped<Tc>()
      .single()

    // And the first row should contain the title
    assertThat(titleCell.text()).contains("Summary")

    // And the second row should contain the content
    assertThat(contentCell.text()).contains(
      "This report follows on from an EMAC Proximity Alert for the above crime and provides proportionate and necessary information in support of that investigation.",
    )

    // And the first row should have a grey background
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("E6E6E6")
  }

  @Test
  fun `it should generate the person details`() {
    // Given a document request for a contextual report
    val request = DocumentRequest.create(
      parameters = givenContextualReportRequestParameters(),
      createdBy = "user123",
      createdAt = Instant.parse("2026-10-01T08:05:00Z"),
    )

    // When the document is generated
    val result = generator.generate(request)

    val document = WordprocessingMLPackage.load(
      ByteArrayInputStream(result.content),
    )

    // Then the document should contain a third table
    val table = document.mainDocumentPart.content
      .unwrapped<Tbl>()
      .drop(2)
      .first()

    val rows = table.content.unwrapped<Tr>()

    // And the table should have two rows
    assertThat(rows).hasSize(7)

    val titleCell = rows[0].content
      .unwrapped<Tc>()
      .single()

    val nameCells = rows[1].content.unwrapped<Tc>()
    val dobCells = rows[2].content.unwrapped<Tc>()
    val pncNumberCells = rows[3].content.unwrapped<Tc>()
    val addressCells = rows[4].content.unwrapped<Tc>()
    val emsIdCells = rows[5].content.unwrapped<Tc>()
    val offenderManagerCells = rows[6].content.unwrapped<Tc>()

    // And the first row should contain the title
    assertThat(titleCell.text()).contains("Person:")

    // And the 2nd to 7th rows should contain the content
    assertThat(nameCells[0].text()).contains("Full Name")
    assertThat(nameCells[1].text()).contains("Jane Doe")
    assertThat(dobCells[0].text()).contains("D.O.B.")
    assertThat(dobCells[1].text()).contains("01/01/1990")
    assertThat(pncNumberCells[0].text()).contains("PNC Number")
    assertThat(pncNumberCells[1].text()).contains("PNC123456")
    assertThat(addressCells[0].text()).contains("Specified Address")
    assertThat(addressCells[1].text()).contains("456 The Avenue, London, UK, AB12 3CD")
    assertThat(emsIdCells[0].text()).contains("EMS ID")
    assertThat(emsIdCells[1].text()).contains("DEVWR123456")
    assertThat(offenderManagerCells[0].text()).contains("Offender Manager")
    assertThat(offenderManagerCells[1].text()).contains("Probation Practitioner")

    // And the first row should have a grey background
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("E6E6E6")
  }

  private fun Tc.text(): String = content
    .unwrapped<P>()
    .flatMap { it.content }
    .unwrapped<R>()
    .flatMap { it.content }
    .unwrapped<Text>()
    .joinToString("") { it.value }

  inline fun <reified T> List<*>.unwrapped(): List<T> = map(XmlUtils::unwrap).filterIsInstance<T>()
}
