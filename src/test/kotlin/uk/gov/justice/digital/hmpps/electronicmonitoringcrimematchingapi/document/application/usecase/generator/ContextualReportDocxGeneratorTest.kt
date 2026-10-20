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

    // Then the document should contain eight tables
    val tables = document.mainDocumentPart.content.unwrapped<Tbl>()

    assertThat(tables).hasSize(8)

    val rows = tables[0].content.unwrapped<Tr>()

    val titleCell = rows[0].content.unwrapped<Tc>().single()

    val contentCell = rows[1].content.unwrapped<Tc>().single()

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

    // Then the document should contain eight tables
    val tables = document.mainDocumentPart.content.unwrapped<Tbl>()

    assertThat(tables).hasSize(8)

    val rows = tables[1].content.unwrapped<Tr>()

    val titleCell = rows[0].content.unwrapped<Tc>().single()
    val contentCell = rows[1].content.unwrapped<Tc>().single()

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

    // Then the document should contain eight tables
    val tables = document.mainDocumentPart.content.unwrapped<Tbl>()

    assertThat(tables).hasSize(8)

    val rows = tables[2].content.unwrapped<Tr>()

    // And the table should have seven rows
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

  @Test
  fun `it should generate the crime details`() {
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

    // Then the document should contain eight tables
    val tables = document.mainDocumentPart.content.unwrapped<Tbl>()

    assertThat(tables).hasSize(8)

    val rows = tables[3].content.unwrapped<Tr>()

    // And the table should have four rows
    assertThat(rows).hasSize(4)

    val titleCell = rows[0].content.unwrapped<Tc>().single()
    val batchIdCells = rows[1].content.unwrapped<Tc>()
    val crimeRefCells = rows[2].content.unwrapped<Tc>()
    val crimeTypeCells = rows[3].content.unwrapped<Tc>()

    // And the first row should contain the title
    assertThat(titleCell.text()).contains("In relation to:")

    // And the 2nd to 4th rows should contain the content
    assertThat(batchIdCells[0].text()).contains("Crime Mapping Batch ID")
    assertThat(batchIdCells[1].text()).contains("AVS20260101")
    assertThat(crimeRefCells[0].text()).contains("Crime Reference Number")
    assertThat(crimeRefCells[1].text()).contains("123456789")
    assertThat(crimeTypeCells[0].text()).contains("Crime Type")
    assertThat(crimeTypeCells[1].text()).contains("Robbery")

    // And the first row should have a grey background
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("E6E6E6")
  }

  @Test
  fun `it should generate the request details`() {
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

    // Then the document should contain eight tables
    val tables = document.mainDocumentPart.content.unwrapped<Tbl>()

    assertThat(tables).hasSize(8)

    val rows = tables[4].content.unwrapped<Tr>()

    // And the table should have five rows
    assertThat(rows).hasSize(5)

    val titleCell = rows[0].content.unwrapped<Tc>().single()
    val constabularyCells = rows[1].content.unwrapped<Tc>()
    val fromCells = rows[2].content.unwrapped<Tc>()
    val dateCells = rows[3].content.unwrapped<Tc>()
    val timeCells = rows[4].content.unwrapped<Tc>()

    // And the first row should contain the title
    assertThat(titleCell.text()).contains("Requested by:")

    // And the 2nd to 5th rows should contain the content
    assertThat(constabularyCells[0].text()).contains("Constabulary")
    assertThat(constabularyCells[1].text()).contains("Metropolitan")
    assertThat(fromCells[0].text()).contains("From")
    assertThat(fromCells[1].text()).contains("Police Officer")
    assertThat(dateCells[0].text()).contains("Date")
    assertThat(dateCells[1].text()).contains("03/10/2026")
    assertThat(timeCells[0].text()).contains("Time")
    assertThat(timeCells[1].text()).contains("11:12:13")

    // And the first row should have a grey background
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("E6E6E6")
  }

  @Test
  fun `it should generate the report period details`() {
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

    // Then the document should contain eight tables
    val tables = document.mainDocumentPart.content.unwrapped<Tbl>()

    assertThat(tables).hasSize(8)

    val rows = tables[5].content.unwrapped<Tr>()

    // And the table should have four rows
    assertThat(rows).hasSize(4)

    val titleCell = rows[0].content.unwrapped<Tc>().single()
    val fromCells = rows[1].content.unwrapped<Tc>()
    val toCells = rows[2].content.unwrapped<Tc>()
    val imagesCells = rows[3].content.unwrapped<Tc>()

    // And the first row should contain the title
    assertThat(titleCell.text()).contains("Request summary:")

    // And the 2nd to 4th rows should contain the content
    assertThat(fromCells[0].text()).contains("Datetime CR requested from")
    assertThat(fromCells[1].text()).contains("01:02 01/10/2026")
    assertThat(toCells[0].text()).contains("Datetime CR requested to")
    assertThat(toCells[1].text()).contains("04:05 02/10/2026")
    assertThat(imagesCells[0].text()).contains("Images")
    assertThat(imagesCells[1].text()).contains("Between Above Date/Time Range")

    // And the first row should have a grey background
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("E6E6E6")
  }

  @Test
  fun `it should generate the further information`() {
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

    // Then the document should contain eight tables
    val tables = document.mainDocumentPart.content.unwrapped<Tbl>()

    assertThat(tables).hasSize(8)

    val rows = tables[6].content.unwrapped<Tr>()

    // And the table should have two rows
    assertThat(rows).hasSize(2)

    val titleCell = rows[0].content.unwrapped<Tc>().single()
    val contentCell = rows[1].content.unwrapped<Tc>().single()

    // And the first row should contain the title
    assertThat(titleCell.text()).contains("Further information (as supplied by Constabulary/Force):")

    // And the 2nd to 4th rows should contain the content
    assertThat(contentCell.text()).contains("More information about the request")

    // And the first row should have a grey background
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("E6E6E6")
  }

  @Test
  fun `it should generate the disclaimer`() {
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

    // Then the document should contain eight tables
    val tables = document.mainDocumentPart.content.unwrapped<Tbl>()

    assertThat(tables).hasSize(8)

    val rows = tables[7].content.unwrapped<Tr>()

    val titleCell = rows[0].content.unwrapped<Tc>().single()
    val contentCell = rows[1].content.unwrapped<Tc>().single()

    // And the first row should contain the title
    assertThat(titleCell.text()).isEqualTo("Disclaimer:")

    // And the second row should contain the content
    assertThat(contentCell.text()).isEqualTo(
      """
        The data disclosed in this report does not confirm that the tag wearer perpetrated or witnessed a crime.
        In addition, it does not rule out other EM tag wearers being in the vicinity of the crime during the window of opportunity.
        The results of this process are also limited to persons tagged as part of the MoJ’s Acquisitive Crime Project and only where there has been an ability to monitor the equipment during the window of opportunity of the crime.
        Examples of an inability to monitor a person include:
        • Where the person has failed to charge the EM tag
        • Where the tag has been removed from the person but is still able to transmit its location
        • Where the equipment has failed due to a technical fault. The technology used (radio frequency transmissions; GPS location monitoring and Location Based Services) has been proven to be reliable over many years. The monitoring equipment is extremely robust and reliable and meets the stringent specifications laid down by the Ministry of Justice. The monitoring equipment is also rigorously tested and audited by EMS, an independent body and the Home Office Scientific Branch. 
      """.trimIndent().trim().replace("\\n".toRegex(), ""),
    )

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
