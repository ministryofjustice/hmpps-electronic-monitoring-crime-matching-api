package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator

import org.assertj.core.api.Assertions.assertThat
import org.docx4j.XmlUtils
import org.docx4j.dml.wordprocessingDrawing.Inline
import org.docx4j.model.pagination.Paginate
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.openpackaging.parts.PartName
import org.docx4j.openpackaging.parts.WordprocessingML.FooterPart
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart
import org.docx4j.wml.Drawing
import org.docx4j.wml.FooterReference
import org.docx4j.wml.HdrFtrRef
import org.docx4j.wml.HeaderReference
import org.docx4j.wml.JcEnumeration
import org.docx4j.wml.P
import org.docx4j.wml.R
import org.docx4j.wml.Tbl
import org.docx4j.wml.Tc
import org.docx4j.wml.Text
import org.docx4j.wml.Tr
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportConfig
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportExhibitTitles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.application.usecase.generator.contextualreport.ContextualReportExhibits
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.DocumentType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.GeneratedDocument
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.SensitivityLabel
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport.ReportPeriod
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.DocumentRequestParametersFixtures.givenContextualReportRequestParameters
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.testutils.FakeMapImageRenderer
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.zip.ZipInputStream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContextualReportDocxGeneratorTest {
  private val generator = ContextualReportDocxGenerator(
    DocumentSensitivityLabeller(
      clock = Clock.fixed(
        Instant.parse("2026-10-08T12:00:00Z"),
        ZoneOffset.UTC,
      ),
    ),
    FakeMapImageRenderer(),
    ContextualReportConfig(),
  )

  private lateinit var result: GeneratedDocument
  private lateinit var document: WordprocessingMLPackage
  private lateinit var tables: List<Tbl>

  @BeforeAll
  fun setUp() {
    val request = DocumentRequest.create(
      parameters = givenContextualReportRequestParameters(),
      createdBy = "user123",
      createdAt = Instant.parse("2026-10-01T08:05:00Z"),
    )

    result = generator.generate(request)

    document = WordprocessingMLPackage.load(
      ByteArrayInputStream(result.content),
    )

    tables = document.mainDocumentPart.content.unwrapped<Tbl>()

    assertThat(tables).hasSize(14)
  }

  @Test
  fun `it should generate a contextual report and return as a byte array`() {
    val bytes = result.content

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

    val properties = document.docPropsCustomPart.jaxbElement.property

    // And the document should be labelled 'OFFICIAL - SENSITIVE'
    assertThat(properties)
      .anySatisfy {
        assertThat(it.name).isEqualTo(
          "MSIP_Label_${SensitivityLabel.OFFICIAL_SENSITIVE.labelId}_Name",
        )
        assertThat(it.lpwstr).isEqualTo(SensitivityLabel.OFFICIAL_SENSITIVE.displayName)
      }

    // And the label sensitivity label should be added to the header and footer
    val references = document.mainDocumentPart
      .jaxbElement.body.sectPr.egHdrFtrReferences

    val headerReference = references
      .filterIsInstance<HeaderReference>()
      .single { it.type == HdrFtrRef.DEFAULT }

    val footerReference = references
      .filterIsInstance<FooterReference>()
      .single { it.type == HdrFtrRef.DEFAULT }

    val headerRelationship = document.mainDocumentPart.relationshipsPart
      .getRelationshipByID(headerReference.id)

    val headerPart = document.parts.get(
      PartName("/word/${headerRelationship.target.removePrefix("/")}"),
    ) as HeaderPart

    val footerRelationship = document.mainDocumentPart.relationshipsPart
      .getRelationshipByID(footerReference.id)

    val footerPart = document.parts.get(
      PartName("/word/${footerRelationship.target.removePrefix("/")}"),
    ) as FooterPart

    assertClassification(
      headerPart.jaxbElement.content.unwrapped<P>().single(),
    )

    assertClassification(
      footerPart.jaxbElement.content.unwrapped<P>().single(),
    )
  }

  @Test
  fun `it should use Arial 11 for report text`() {
    val normalStyle = document.mainDocumentPart.styleDefinitionsPart.getStyleById("Normal")

    assertThat(normalStyle.rPr.rFonts.ascii).isEqualTo("Arial")
    assertThat(normalStyle.rPr.sz.`val`).isEqualTo(BigInteger.valueOf(22))
    assertThat(normalStyle.rPr.szCs.`val`).isEqualTo(BigInteger.valueOf(22))

    val positionTable = tables[10]
      .content.unwrapped<Tr>()[1]
      .content.unwrapped<Tc>().first()
      .content.unwrapped<Tbl>().single()

    positionTable.content.unwrapped<Tr>()
      .flatMap { it.content.unwrapped<Tc>() }
      .flatMap { it.content.unwrapped<P>() }
      .flatMap { it.content.unwrapped<R>() }
      .forEach { run ->
        assertThat(run.rPr.sz.`val`).isEqualTo(BigInteger.valueOf(22))
      }
  }

  @Test
  fun `it should generate top-level full width tables`() {
    assertThat(tables).hasSize(14)

    tables.forEach { table ->
      assertThat(table.tblPr.tblW.type).isEqualTo("dxa")
      assertThat(table.tblPr.tblW.w)
        .isEqualTo(BigInteger.valueOf(9026))
    }
  }

  @Test
  fun `it should generate the contextual report header`() {
    val headerTable = tables[0]
    val rows = headerTable.content.unwrapped<Tr>()

    // Then the table should have two rows
    assertThat(rows).hasSize(2)

    val titleCell = rows[0].content.unwrapped<Tc>().single()
    val contentCell = rows[1].content.unwrapped<Tc>().single()

    // And the first row should contain the title
    assertThat(titleCell.text()).contains("Date: 01/10/2026")

    // And the second row should contain the content
    assertThat(contentCell.text()).contains(
      "Contextual Movement Report for Crime Reference Number - 123456789",
    )

    // And the first row should have a grey background
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("D9D9D9")
  }

  @Test
  fun `it should generate the contextual report summary`() {
    val summaryTable = tables[1]
    val rows = summaryTable.content.unwrapped<Tr>()

    // Then the table should have two rows
    assertThat(rows).hasSize(2)

    val titleCell = rows[0].content.unwrapped<Tc>().single()
    val contentCell = rows[1].content.unwrapped<Tc>().single()

    // And the first row should contain the title
    assertThat(titleCell.text()).contains("Summary")

    // And the second row should contain the content
    assertThat(contentCell.text()).contains(
      "This report follows on from an EMAC Proximity Alert for the above crime and provides proportionate and necessary information in support of that investigation.",
    )

    // And the first row should have a grey background
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("D9D9D9")
  }

  @Test
  fun `it should generate the person details`() {
    val personDetailsTable = tables[2]
    val rows = personDetailsTable.content.unwrapped<Tr>()

    // Then the table should have six rows
    assertThat(rows).hasSize(6)

    val titleCell = rows[0].content
      .unwrapped<Tc>()
      .single()

    val nameCells = rows[1].content.unwrapped<Tc>()
    val dobCells = rows[2].content.unwrapped<Tc>()
    val pncNumberCells = rows[3].content.unwrapped<Tc>()
    val addressCells = rows[4].content.unwrapped<Tc>()
    val emsIdCells = rows[5].content.unwrapped<Tc>()

    // And the first row should contain the title
    assertThat(titleCell.text()).contains("Person:")

    // And the 2nd to 6th rows should contain the content
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

    // And the first row should have a grey background
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("D9D9D9")
  }

  @Test
  fun `it should generate the crime details`() {
    val crimeDetailsTable = tables[3]
    val rows = crimeDetailsTable.content.unwrapped<Tr>()

    // Then the table should have four rows
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
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("D9D9D9")
  }

  @Test
  fun `it should generate the request details`() {
    val requestDetailsTable = tables[4]
    val rows = requestDetailsTable.content.unwrapped<Tr>()

    // Then the table should have five rows
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
    assertThat(timeCells[1].text()).contains("12:12:13")

    // And the first row should have a grey background
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("D9D9D9")
  }

  @Test
  fun `it should generate the report period details`() {
    val reportPeriodDetailsTable = tables[5]
    val rows = reportPeriodDetailsTable.content.unwrapped<Tr>()

    // Then the table should have four rows
    assertThat(rows).hasSize(4)

    val titleCell = rows[0].content.unwrapped<Tc>().single()
    val fromCells = rows[1].content.unwrapped<Tc>()
    val toCells = rows[2].content.unwrapped<Tc>()
    val imagesCells = rows[3].content.unwrapped<Tc>()

    // And the first row should contain the title
    assertThat(titleCell.text()).contains("Request summary:")

    // And the 2nd to 4th rows should contain the content
    assertThat(fromCells[0].text()).contains("Datetime CR requested from")
    assertThat(fromCells[1].text()).contains("01/10/2026 02:02")
    assertThat(toCells[0].text()).contains("Datetime CR requested to")
    assertThat(toCells[1].text()).contains("02/10/2026 05:05")
    assertThat(imagesCells[0].text()).contains("Images")
    assertThat(imagesCells[1].text()).contains("Between Above Date/Time Range")

    // And the first row should have a grey background
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("D9D9D9")
  }

  @Test
  fun `it should generate the further information`() {
    val furtherInformationTable = tables[6]
    val rows = furtherInformationTable.content.unwrapped<Tr>()

    // Then the table should have two rows
    assertThat(rows).hasSize(2)

    val titleCell = rows[0].content.unwrapped<Tc>().single()
    val contentCell = rows[1].content.unwrapped<Tc>().single()

    // And the first row should contain the title
    assertThat(titleCell.text()).contains("Further information (as supplied by Constabulary/Force):")

    // And the 2nd to 4th rows should contain the content
    assertThat(contentCell.text()).contains("More information about the request")

    // And the first row should have a grey background
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("D9D9D9")
  }

  @Test
  fun `it should generate the disclaimer`() {
    val disclaimerTable = tables[7]
    val rows = disclaimerTable.content.unwrapped<Tr>()

    // Then the table should have two rows
    assertThat(rows).hasSize(2)

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
        Where the person has failed to charge the EM tag
        Where the tag has been removed from the person but is still able to transmit its location
        Where the equipment has failed due to a technical fault. The technology used (radio frequency transmissions; GPS location monitoring and Location Based Services) has been proven to be reliable over many years. The monitoring equipment is extremely robust and reliable and meets the stringent specifications laid down by the Ministry of Justice. The monitoring equipment is also rigorously tested and audited by EMS, an independent body and the Home Office Scientific Branch. 
      """.trimIndent().trim().replace("\\n".toRegex(), ""),
    )

    // And the first row should have a grey background
    assertThat(titleCell.tcPr.shd.fill).isEqualTo("D9D9D9")
  }

  @Test
  fun `it should generate the witness statement`() {
    val witnessStatementTable = tables[8]
    val rows = witnessStatementTable.content.unwrapped<Tr>()

    // Then the table should have four rows
    assertThat(rows).hasSize(4)

    // And the header should contain the relevant legislation
    val header = rows[0].content.unwrapped<Tc>().single()
    assertThat(header.text()).isEqualTo(
      "WITNESS STATEMENT" +
        "(CJ Act 1967, s.9; MC Act 1980, ss.5A (3) (a) and 5B; Criminal Procedure Rules 2005, Rule 27.1)",
    )
    assertThat(header.tcPr.shd.fill).isEqualTo("D9D9D9")

    // And the witness details should be populated
    val witnessDetails = rows[1].content.unwrapped<Tc>()

    assertThat(witnessDetails).hasSize(2)
    assertThat(witnessDetails[0].text()).isEqualTo("Statement of: John Smith")
    assertThat(witnessDetails[1].text()).isEqualTo("Occupation: AC Hub Manager")

    // And the declaration should be correct and contain the manager's details
    val declaration = rows[2].content.unwrapped<Tc>().single()

    assertThat(declaration.text()).isEqualTo(
      "Age: Over 18" +
        "This statement consisting of ${Paginate.compute(document, null).pageCount} pages, signed by me, " +
        "is true to the best of my knowledge and belief. I make it knowing that, if it is given in evidence, " +
        "I shall be liable to prosecution if I have willfully stated in it, anything that I know to be false or " +
        "do not believe to be true." +
        "Signature:" +
        "Manager's name: John Smith" +
        "Date: 01/10/2026" +
        "Email: mojacquisitivecrimehub@justice.gov.uk (preferred communication method)" +
        "Address: 102 Petty France, Westminster, London, SW1H 9AJ",
    )

    // And the statement should be correct
    val statement = rows[3].content.unwrapped<Tc>().single()

    assertThat(statement.text()).isEqualTo(
      "I am currently employed by the Ministry of Justice (MoJ) as a Manager within the " +
        "Electronic Monitoring Acquisitive Crime Hub (EMAC Hub)." +
        "Further to the statement dated 01/09/2026 regarding an allegation of Robbery " +
        "(Crime Reference No. 123456789). On 03/10/2026 the EMAC Hub received a request in connection with the above " +
        "investigation for movement data for Jane Doe, 01/01/1990. The request required the following:" +
        "Subject Jane Doe movements from 01/10/2026 02:02 to 02/10/2026 05:05" +
        "As a result of this request, I further produce the following screen shot(s) and data table " +
        "which document the subject’s movements as requested above." +
        "Exhibit EMAC/04: Key for interpreting symbols on crime map" +
        "Exhibit EMAC/05: Key for interpreting location data table" +
        "Exhibit EMAC/06: Image of all locations for Jane Doe, between 01/10/2026 01:00 and 01/10/2026 01:00" +
        "Exhibit EMAC/07: Image of the locations for Jane Doe, between 01/10/2026 01:00 and 01/10/2026 01:00" +
        "Exhibit EMAC/08: Table of Jane Doe's locations, between 01/10/2026 01:00 and 01/10/2026 01:00" +
        "Signature:" +
        "Manager's name: John Smith" +
        "Date: 01/10/2026" +
        "Email: mojacquisitivecrimehub@justice.gov.uk (preferred communication method)" +
        "Address: 102 Petty France, Westminster, London, SW1H 9AJ",
    )
  }

  @Test
  fun `it should generate the example map exhibit`() {
    val exampleMapExhibitTable = tables[9]
    val rows = exampleMapExhibitTable.content.unwrapped<Tr>()

    // Then the table should have two rows
    assertThat(rows).hasSize(2)

    // And the header should contain the artefact reference
    val header = rows[0].content.unwrapped<Tc>().single()
    assertThat(header.text()).isEqualTo(
      "Exhibit EMAC/04: Key for interpreting symbols on crime map",
    )
    // No text run in the map exhibit title should have bold formatting enabled.
    assertThat(header.content.unwrapped<P>().single().content.unwrapped<R>()).noneMatch { it.rPr?.b?.isVal == true }
    assertThat(header.tcPr.shd.fill).isEqualTo("D9D9D9")

    // And the content should contain the example image
    val content = rows[1].content.unwrapped<Tc>().single()

    val drawings = content.content
      .unwrapped<P>()
      .flatMap { it.content.unwrapped<R>() }
      .flatMap { it.content.unwrapped<Drawing>() }

    assertThat(drawings).hasSize(1)

    val inline = drawings.single().anchorOrInline.single() as Inline

    assertThat(inline.docPr.descr).isEqualTo(
      "Exhibit EMAC/04: Key for interpreting symbols on crime map",
    )
  }

  @Test
  fun `it should generate the example table exhibit`() {
    val exampleTableExhibitTable = tables[10]
    val rows = exampleTableExhibitTable.content.unwrapped<Tr>()

    // Then the table should have two rows
    assertThat(rows).hasSize(2)

    // And the header should contain the artefact reference
    val header = rows[0].content.unwrapped<Tc>().single()
    assertThat(header.text()).isEqualTo(
      "Exhibit EMAC/05: Key for interpreting location data table",
    )
    // No text run in the table exhibit title should have bold formatting enabled.
    assertThat(header.content.unwrapped<P>().single().content.unwrapped<R>()).noneMatch { it.rPr?.b?.isVal == true }
    assertThat(header.tcPr.shd.fill).isEqualTo("D9D9D9")

    // And the content should contain the example table
    val content = rows[1].content.unwrapped<Tc>().single()
    val positionTable = content.content.unwrapped<Tbl>().single()

    val positionRows = positionTable.content.unwrapped<Tr>()
    assertThat(positionRows).hasSize(4)

    // And the description row should contain the explanatory text
    val descriptionCells = positionRows[0].content.unwrapped<Tc>()

    assertThat(descriptionCells).hasSize(7)
    assertThat(descriptionCells.map { it.text() }).containsExactly(
      "Represents the date and time of recorded location",
      "Represents the latitude of recorded location",
      "Represents the longitude of recorded location",
      "Represents the confidence circle of recorded location",
      "Represents the speed between consecutive recorded locations 0.1 Km/h (i.e., 21 = 2.1 km/h)",
      "Represents the direction in degrees between consecutive recorded locations",
      "Correlates to numbered location points on the corresponding map and the data in the table of that row",
    )

    assertThat(descriptionCells.map { it.tcPr.shd.fill })
      .containsOnly("62B3F5")

    // And the column headings should be correct
    val headingCells = positionRows[1].content.unwrapped<Tc>()

    assertThat(headingCells).hasSize(7)
    assertThat(headingCells.map { it.text() }).containsExactly(
      "DATE/TIME",
      "LATITUDE",
      "LONGITUDE",
      "CONFIDENCE CIRCLE",
      "SPEED",
      "DIRECTION",
      "SEQUENCE NO.",
    )

    assertThat(headingCells.map { it.tcPr.shd.fill })
      .containsOnly("306DBF")

    // And the table should contain the example positions
    val firstPosition = positionRows[2].content.unwrapped<Tc>()

    assertThat(firstPosition.map { it.text() }).containsExactly(
      "01/01/2021 00:00:00",
      "50.123456",
      "-0.123456",
      "50",
      "21",
      "30",
      "1",
    )

    val secondPosition = positionRows[3].content.unwrapped<Tc>()

    assertThat(secondPosition.map { it.text() }).containsExactly(
      "01/01/2021 00:01:00",
      "50.234567",
      "-0.234567",
      "75",
      "35",
      "35",
      "2",
    )
  }

  @Test
  fun `it should generate a map exhibit for the first segment`() {
    val firstMapExhibitTable = tables[12]
    val rows = firstMapExhibitTable.content.unwrapped<Tr>()

    // Then the table should have two rows
    assertThat(rows).hasSize(2)

    // And the header should contain the artefact reference
    val header = rows[0].content.unwrapped<Tc>().single()
    assertThat(header.text()).isEqualTo(
      "Exhibit EMAC/07: Image of the locations for Jane Doe, between 01/10/2026 01:00 and 01/10/2026 01:00",
    )
    assertThat(header.tcPr.shd.fill).isEqualTo("D9D9D9")

    // And the content should contain an image
    val content = rows[1].content.unwrapped<Tc>().single()

    val drawings = content.content
      .unwrapped<P>()
      .flatMap { it.content.unwrapped<R>() }
      .flatMap { it.content.unwrapped<Drawing>() }

    assertThat(drawings).hasSize(1)

    val inline = drawings.single().anchorOrInline.single() as Inline

    assertThat(inline.docPr.descr).isEqualTo(
      "Exhibit EMAC/07: Image of the locations for Jane Doe, between 01/10/2026 01:00 and 01/10/2026 01:00",
    )
  }

  @Test
  fun `it should generate a table exhibit for the first segment`() {
    val firstTableExhibitTable = tables[13]
    val rows = firstTableExhibitTable.content.unwrapped<Tr>()

    // Then the table should have two rows
    assertThat(rows).hasSize(2)

    // And the header should contain the artefact reference
    val header = rows[0].content.unwrapped<Tc>().single()
    assertThat(header.text()).isEqualTo(
      "Exhibit EMAC/08: Table of Jane Doe's locations, between 01/10/2026 01:00 and 01/10/2026 01:00",
    )
    assertThat(header.tcPr.shd.fill).isEqualTo("D9D9D9")

    // And the content should contain the example table
    val content = rows[1].content.unwrapped<Tc>().single()
    val positionTable = content.content.unwrapped<Tbl>().single()

    val positionRows = positionTable.content.unwrapped<Tr>()
    assertThat(positionRows).hasSize(3)

    // And the column headings should be correct
    val headingCells = positionRows[0].content.unwrapped<Tc>()

    assertThat(headingCells).hasSize(7)
    assertThat(headingCells.map { it.text() }).containsExactly(
      "DATE/TIME",
      "LATITUDE",
      "LONGITUDE",
      "CONFIDENCE CIRCLE",
      "SPEED",
      "DIRECTION",
      "SEQUENCE NO.",
    )

    assertThat(headingCells.map { it.tcPr.shd.fill })
      .containsOnly("306DBF")

    // And the table should contain the position in the segment
    val firstPosition = positionRows[1].content.unwrapped<Tc>()

    assertThat(firstPosition.map { it.text() }).containsExactly(
      "01/10/2026 01:00:00",
      "51.5074",
      "-0.1278",
      "5",
      "10",
      "90",
      "1",
    )
    assertThat(positionRows.last().content.unwrapped<Tc>().single().text())
      .isEqualTo("No GNSS data between 01/10/2026 01:00:00 and 02/10/2026 05:05:06")
  }

  @Test
  fun `it should have Missing GNSS gap notices that span every column and appear in order in the affected segment tables`() {
    // Given two position segments with gaps in every possible location; at the beginning of the report window, between positions, between segments and at the end of the report window
    val period = ReportPeriod(Instant.parse("2026-09-30T22:58:37Z"), Instant.parse("2026-09-30T23:10:47Z"))
    val fixture = givenContextualReportRequestParameters(reportPeriod = period)
    val position = fixture.positionSegments.single().single()
    val first = position.copy(gpsDate = period.start.plusSeconds(120))
    val second = first.copy(gpsDate = first.gpsDate.plusSeconds(60), sequenceNumber = 2)
    val third = second.copy(gpsDate = second.gpsDate.plusSeconds(60).plusMillis(1), sequenceNumber = 3)
    val fourth = first.copy(gpsDate = period.end.minusSeconds(300), sequenceNumber = 4)
    val parameters = fixture.copy(positionSegments = listOf(listOf(first, second, third), listOf(fourth)))
    val config = ContextualReportConfig()
    val exhibits = WordprocessingMLPackage.createPackage()
    val renderer = FakeMapImageRenderer()

    // When the exhibits are generated
    ContextualReportExhibits(parameters, config, ContextualReportExhibitTitles(parameters, config).titles, renderer)
      .addTo(exhibits)

    // Then a map image should be requested for every segment and each one should include tracks
    assertThat(renderer.requests.filter { it.includeTrack }.map { it.positions })
      .containsExactlyElementsOf(parameters.positionSegments)
    assertThat(renderer.requests.map { it.viewportPaddingPx }).containsExactly(80, 80, 80)

    fun unwrapTableRows(table: Tbl): List<String> {
      val contentCell = table.content.unwrapped<Tr>().last().content.unwrapped<Tc>().single()
      val positionTable = contentCell.content.unwrapped<Tbl>().single()
      return positionTable.content.unwrapped<Tr>().drop(1).map { row ->
        val cells = row.content.unwrapped<Tc>()
        if (cells.size == 1) {
          val gapCell = cells.single()
          assertThat(gapCell.tcPr.gridSpan.`val`).isEqualTo(7.toBigInteger())
          assertThat(gapCell.content.unwrapped<P>().single().pPr.jc.`val`).isEqualTo(JcEnumeration.CENTER)
        } else {
          assertThat(cells).hasSize(7)
        }
        cells.last().text() // Either the gap notice or the recorded sequence number.
      }
    }

    // And the table contains the expected "Missing GNSS" gap notices
    val exhibitTables = exhibits.mainDocumentPart.content.unwrapped<Tbl>()
    val firstSegmentPositionTable = exhibitTables[4].content.unwrapped<Tr>().last()
      .content.unwrapped<Tc>().single().content.unwrapped<Tbl>().single()
    assertThat(firstSegmentPositionTable.content.unwrapped<Tr>()[2].content.unwrapped<Tc>().first().text())
      .isEqualTo("01/10/2026 00:00:37")
    val betweenSegments = "No GNSS data between 01/10/2026 00:02:37 and 01/10/2026 00:05:47"
    assertThat(unwrapTableRows(exhibitTables[4])).containsExactly(
      "No GNSS data between 30/09/2026 23:58:37 and 01/10/2026 00:00:37",
      "1",
      "2",
      "No GNSS data between 01/10/2026 00:01:37 and 01/10/2026 00:02:37",
      "3",
      betweenSegments,
    )
    assertThat(unwrapTableRows(exhibitTables[6])).containsExactly(
      betweenSegments,
      "4",
      "No GNSS data between 01/10/2026 00:05:47 and 01/10/2026 00:10:47",
    )
  }

  private fun Tc.text(): String = content
    .unwrapped<P>()
    .flatMap { it.content }
    .unwrapped<R>()
    .flatMap { it.content }
    .unwrapped<Text>()
    .joinToString("") { it.value }

  inline fun <reified T> List<*>.unwrapped(): List<T> = map(XmlUtils::unwrap).filterIsInstance<T>()

  private fun assertClassification(paragraph: P) {
    val runs = paragraph.content.unwrapped<R>()
    val text = runs
      .flatMap { it.content.unwrapped<Text>() }
      .joinToString("") { it.value }

    assertThat(text).isEqualTo("OFFICIAL - SENSITIVE")
    assertThat(runs.single().rPr.rFonts.ascii).isEqualTo("Arial")
    assertThat(runs.single().rPr.sz.`val`).isEqualTo(BigInteger.valueOf(22))

    assertThat(paragraph.pPr.jc.`val`)
      .isEqualTo(JcEnumeration.CENTER)
  }
}
