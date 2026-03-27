package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createCsvRow
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeBatchEmailAttachmentIngestionErrorType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.validation.EmailAttachmentIngestionError
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.collections.listOf

@ActiveProfiles("test")
class CrimeBatchCsvServiceTest {
  private lateinit var service: CrimeBatchCsvService

  @BeforeEach
  fun setup() {
    service = CrimeBatchCsvService()
  }

  @Test
  fun `it should parse a valid crime`() {
    val crimeData = createCsvRow().byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).isEqualTo(
      listOf(
        CrimeRecordRequest(
          policeForce = PoliceForce.METROPOLITAN,
          crimeTypeId = CrimeType.TOMV,
          batchId = "MPS20250126",
          crimeReference = "CRI00000001",
          crimeDateTimeFrom = LocalDateTime.of(2025, 1, 25, 8, 30).toInstant(ZoneOffset.UTC),
          crimeDateTimeTo = LocalDateTime.of(2025, 1, 25, 8, 30).toInstant(ZoneOffset.UTC),
          easting = null,
          northing = null,
          latitude = 54.73241,
          longitude = -1.38542,
          crimeText = "",
        ),
      ),
    )
    assertThat(parseResult.errors).hasSize(0)
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @Test
  fun `it should ignore an empty row`() {
    val crimeData = "".byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).hasSize(0)
    assertThat(parseResult.recordCount).isEqualTo(0)
  }

  @Test
  fun `it should not parse a row with too few columns`() {
    val crimeData = ",,".byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = null,
          crimeTypeId = null,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_COLUMN_COUNT,
          field = null,
        ),
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @Test
  fun `it should not parse a row with too many columns`() {
    val crimeData = ",,,,,,,,,,,,,".byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = null,
          crimeTypeId = null,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_COLUMN_COUNT,
          field = null,
        ),
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @ParameterizedTest(name = "it should parse all valid police forces - {0} -> {1}")
  @MethodSource("policeForceValues")
  fun `it should parse all valid police forces`(csvValue: String, enumValue: PoliceForce) {
    val crimeData = createCsvRow(policeForce = csvValue, batchId = enumValue.code + "20250109").byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(1)
    assertThat(parseResult.records[0].policeForce).isEqualTo(enumValue)
    assertThat(parseResult.errors).hasSize(0)
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @Test
  fun `it should not parse an invalid police force`() {
    val crimeData = createCsvRow(policeForce = "invalid police force").byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = CrimeType.TOMV,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_POLICE_FORCE,
          field = "policeForce",
          value = "invalid police force",
        ),
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @ParameterizedTest(name = "it should parse all valid crime types - {0} -> {1}")
  @MethodSource("crimeTypeValues")
  fun `it should parse all valid crime types`(csvValue: String, enumValue: CrimeType) {
    val crimeData = createCsvRow(crimeTypeId = csvValue).byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(1)
    assertThat(parseResult.records[0].crimeTypeId).isEqualTo(enumValue)
    assertThat(parseResult.errors).hasSize(0)
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @Test
  fun `it should not parse an invalid crime type`() {
    val crimeData = createCsvRow(crimeTypeId = "invalid crime type").byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = null,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_CRIME_TYPE,
          field = "crimeType",
          value = "invalid crime type",
        ),
      ),
    )
  }

  @Test
  fun `it should not parse an empty batch ID`() {
    val crimeData = createCsvRow(batchId = "").byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = CrimeType.TOMV,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.MISSING_BATCH_ID,
          field = "batchId",
        ),
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @Test
  fun `it should not parse an invalid Police Force in batch ID`() {
    val crimeData = createCsvRow(batchId = "MPS20250101", policeForce = PoliceForce.BEDFORDSHIRE.value).byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = CrimeType.TOMV,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_BATCH_ID_FORMAT,
          field = "batchId",
          value = "MPS20250101",
        ),
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @Test
  fun `it should not parse an invalid date in batch ID`() {
    val crimeData = createCsvRow(batchId = "MPS20253001").byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = CrimeType.TOMV,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_BATCH_ID_DATE,
          field = "batchId",
          value = "MPS20253001",
        ),
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @Test
  fun `it should not parse an invalid crime reference`() {
    val crimeData = createCsvRow(crimeReference = "").byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = null,
          crimeTypeId = CrimeType.TOMV,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.MISSING_CRIME_REFERENCE,
          field = "crimeReference",
        ),
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @Test
  fun `it should not parse an invalid crime date from`() {
    val crimeData = createCsvRow(crimeDateTimeFrom = "").byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = CrimeType.TOMV,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_FROM_DATE_FORMAT,
          field = "dateFrom",
          value = "",
        ),
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @Test
  fun `it should not parse an invalid crime date to`() {
    val crimeData = createCsvRow(crimeDateTimeTo = "").byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = CrimeType.TOMV,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_TO_DATE_FORMAT,
          field = "dateTo",
          value = "",
        ),
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @Test
  fun `it should not parse a crime if dateTo is before dateFrom`() {
    val crimeData = createCsvRow(
      crimeDateTimeFrom = "20250225083000",
      crimeDateTimeTo = "20250125083000",
    ).byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = CrimeType.TOMV,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.CRIME_DATE_TIME_TO_AFTER_FROM,
          field = "dateTo",
          value = "20250125083000",
        ),
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @Test
  fun `it should not parse a crime if crime date windows exceeds 12 hours`() {
    val crimeData = createCsvRow(
      crimeDateTimeFrom = "20250125083000",
      crimeDateTimeTo = "20250325083000",
    ).byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = CrimeType.TOMV,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.CRIME_DATE_TIME_EXCEEDS_WINDOW,
          field = "dateTo",
          value = "1416",
        ),
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @Test
  fun `it should return many errors if multiple fields are invalid`() {
    val crimeData = createCsvRow(
      policeForce = "invalid police force",
      crimeTypeId = "invalid crime type",
      crimeDateTimeFrom = "",
      crimeDateTimeTo = "",
    ).byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = null,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_POLICE_FORCE,
          field = "policeForce",
          value = "invalid police force",
        ),
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = null,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_CRIME_TYPE,
          field = "crimeType",
          value = "invalid crime type",
        ),
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = null,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_FROM_DATE_FORMAT,
          field = "dateFrom",
          value = "",
        ),
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = null,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_TO_DATE_FORMAT,
          field = "dateTo",
          value = "",
        ),
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @Test
  fun `it should be possible to identify which row the error was on`() {
    val crimeData = listOf(
      createCsvRow(),
      createCsvRow(crimeTypeId = "invalid"),
      createCsvRow(crimeDateTimeFrom = "invalid"),
    ).joinToString("\n").byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)

    assertThat(parseResult.records).hasSize(1)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 2,
          crimeReference = "CRI00000001",
          crimeTypeId = null,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_CRIME_TYPE,
          field = "crimeType",
          value = "invalid",
        ),
        EmailAttachmentIngestionError(
          rowNumber = 3,
          crimeReference = "CRI00000001",
          crimeTypeId = CrimeType.TOMV,
          errorType = CrimeBatchEmailAttachmentIngestionErrorType.INVALID_FROM_DATE_FORMAT,
          field = "dateFrom",
          value = "invalid",
        ),
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(3)
  }

  @Test
  fun `it should not parse when multiple location data types are provided`() {
    val crimeData = createCsvRow(easting = "1", northing = "1", latitude = "50", longitude = "1").byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)
    assertThat(parseResult.records).hasSize(0)

    assertThat(parseResult.errors).contains(
      EmailAttachmentIngestionError(
        rowNumber = 1,
        crimeReference = "CRI00000001",
        crimeTypeId = CrimeType.TOMV,
        errorType = CrimeBatchEmailAttachmentIngestionErrorType.MULTIPLE_LOCATION_DATA_TYPES,
        field = "easting",
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  @ParameterizedTest(name = "easting={0}, northing={1}, lat={2}, long={3}, field={4}, errorType={5}, value={6}")
  @MethodSource("invalidLocationValues")
  fun `it should not parse invalid location data`(
    easting: String,
    northing: String,
    latitude: String,
    longitude: String,
    field: String?,
    errorType: CrimeBatchEmailAttachmentIngestionErrorType,
    value: String?,
  ) {
    val crimeData = createCsvRow(easting = easting, northing = northing, latitude = latitude, longitude = longitude).byteInputStream()
    val parseResult = service.parseCsvFile(crimeData)
    assertThat(parseResult.records).hasSize(0)
    assertThat(parseResult.errors).isEqualTo(
      listOf(
        EmailAttachmentIngestionError(
          rowNumber = 1,
          crimeReference = "CRI00000001",
          crimeTypeId = CrimeType.TOMV,
          errorType = errorType,
          field = field,
          value = value,
        ),
      ),
    )
    assertThat(parseResult.recordCount).isEqualTo(1)
  }

  companion object {
    @JvmStatic
    fun policeForceValues() = listOf(
      Arguments.of("AVON_AND_SOMERSET", PoliceForce.AVON_AND_SOMERSET),
      Arguments.of("BEDFORDSHIRE", PoliceForce.BEDFORDSHIRE),
      Arguments.of("CHESHIRE", PoliceForce.CHESHIRE),
      Arguments.of("CITY_OF_LONDON", PoliceForce.CITY_OF_LONDON),
      Arguments.of("CUMBRIA", PoliceForce.CUMBRIA),
      Arguments.of("DERBYSHIRE", PoliceForce.DERBYSHIRE),
      Arguments.of("DURHAM", PoliceForce.DURHAM),
      Arguments.of("ESSEX", PoliceForce.ESSEX),
      Arguments.of("GLOUCESTERSHIRE", PoliceForce.GLOUCESTERSHIRE),
      Arguments.of("GWENT", PoliceForce.GWENT),
      Arguments.of("HAMPSHIRE", PoliceForce.HAMPSHIRE),
      Arguments.of("HERTFORDSHIRE", PoliceForce.HERTFORDSHIRE),
      Arguments.of("HUMBERSIDE", PoliceForce.HUMBERSIDE),
      Arguments.of("KENT", PoliceForce.KENT),
      Arguments.of("METROPOLITAN", PoliceForce.METROPOLITAN),
      Arguments.of("NORTH_WALES", PoliceForce.NORTH_WALES),
      Arguments.of("NOTTINGHAMSHIRE", PoliceForce.NOTTINGHAMSHIRE),
      Arguments.of("SUSSEX", PoliceForce.SUSSEX),
      Arguments.of("WEST_MIDLANDS", PoliceForce.WEST_MIDLANDS),
    )

    @JvmStatic
    fun crimeTypeValues() = listOf(
      Arguments.of("RB", CrimeType.RB),
      Arguments.of("BIAD", CrimeType.BIAD),
      Arguments.of("AB", CrimeType.AB),
      Arguments.of("BOTD", CrimeType.BOTD),
      Arguments.of("TOMV", CrimeType.TOMV),
      Arguments.of("TFP", CrimeType.TFP),
      Arguments.of("TFMV", CrimeType.TFMV),
    )

    @JvmStatic
    fun invalidLocationValues() = listOf(
      Arguments.of("-1", "1", "", "", "easting", CrimeBatchEmailAttachmentIngestionErrorType.INVALID_LOCATION_DATA_RANGE, "-1.0"),
      Arguments.of("600001", "1", "", "", "easting", CrimeBatchEmailAttachmentIngestionErrorType.INVALID_LOCATION_DATA_RANGE, "600001.0"),
      Arguments.of("1", "", "", "", "easting", CrimeBatchEmailAttachmentIngestionErrorType.DEPENDENT_LOCATION_DATA, "1"),
      Arguments.of("1", "-1", "", "", "northing", CrimeBatchEmailAttachmentIngestionErrorType.INVALID_LOCATION_DATA_RANGE, "-1.0"),
      Arguments.of("1", "1300001", "", "", "northing", CrimeBatchEmailAttachmentIngestionErrorType.INVALID_LOCATION_DATA_RANGE, "1300001.0"),
      Arguments.of("", "1", "", "", "northing", CrimeBatchEmailAttachmentIngestionErrorType.DEPENDENT_LOCATION_DATA, "1"),
      Arguments.of("", "", "62", "1", "latitude", CrimeBatchEmailAttachmentIngestionErrorType.INVALID_LOCATION_DATA_RANGE, "62.0"),
      Arguments.of("", "", "49", "1", "latitude", CrimeBatchEmailAttachmentIngestionErrorType.INVALID_LOCATION_DATA_RANGE, "49.0"),
      Arguments.of("", "", "50", "", "latitude", CrimeBatchEmailAttachmentIngestionErrorType.DEPENDENT_LOCATION_DATA, "50"),
      Arguments.of("", "", "50", "-9.0", "longitude", CrimeBatchEmailAttachmentIngestionErrorType.INVALID_LOCATION_DATA_RANGE, "-9.0"),
      Arguments.of("", "", "50", "3", "longitude", CrimeBatchEmailAttachmentIngestionErrorType.INVALID_LOCATION_DATA_RANGE, "3.0"),
      Arguments.of("", "", "", "1", "longitude", CrimeBatchEmailAttachmentIngestionErrorType.DEPENDENT_LOCATION_DATA, "1"),
      Arguments.of("", "", "", "", null, CrimeBatchEmailAttachmentIngestionErrorType.MISSING_LOCATION_DATA, null),
    )
  }
}
