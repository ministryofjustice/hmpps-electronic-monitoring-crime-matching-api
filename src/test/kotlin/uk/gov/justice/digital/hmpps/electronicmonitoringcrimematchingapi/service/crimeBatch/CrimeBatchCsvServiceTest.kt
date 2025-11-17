package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import jakarta.validation.Validation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createCsvRow
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.GeodeticDatum
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import java.time.LocalDateTime
import kotlin.collections.listOf

@ActiveProfiles("test")
class CrimeBatchCsvServiceTest {
  private lateinit var service: CrimeBatchCsvService
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @BeforeEach
  fun setup() {
    service = CrimeBatchCsvService(validator)
  }

  @Test
  fun `it should parse a valid crime`() {
    val crimeData = createCsvRow().byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).isEqualTo(
      listOf(
        CrimeRecordDto(
          policeForce = PoliceForce.METROPOLITAN,
          crimeTypeId = CrimeType.TOMV,
          crimeReference = "CRI00000001",
          crimeDateTimeFrom = LocalDateTime.of(2025, 1, 25, 8, 30),
          crimeDateTimeTo = LocalDateTime.of(2025, 1, 25, 8, 30),
          easting = null,
          northing = null,
          latitude = 54.73241,
          longitude = -1.38542,
          datum = GeodeticDatum.WGS84,
          crimeText = "",
        ),
      ),
    )
    assertThat(errors).hasSize(0)
  }

  @Test
  fun `it should ignore an empty row`() {
    val crimeData = "".byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(0)
    assertThat(errors).hasSize(0)
  }

  @Test
  fun `it should not parse a row with too few columns`() {
    val crimeData = ",,".byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(0)
    assertThat(errors).isEqualTo(
      listOf("Incorrect number of columns on row 1."),
    )
  }

  @Test
  fun `it should not parse a row with too many columns`() {
    val crimeData = ",,,,,,,,,,,,,".byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(0)
    assertThat(errors).isEqualTo(
      listOf("Incorrect number of columns on row 1."),
    )
  }

  @ParameterizedTest(name = "it should parse all valid police forces - {0} -> {1}")
  @MethodSource("policeForceValues")
  fun `it should parse all valid police forces`(csvValue: String, enumValue: PoliceForce) {
    val crimeData = createCsvRow(policeForce = csvValue).byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(1)
    assertThat(crimes[0].policeForce).isEqualTo(enumValue)
    assertThat(errors).hasSize(0)
  }

  @Test
  fun `it should not parse an invalid police force`() {
    val crimeData = createCsvRow(policeForce = "invalid police force").byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(0)
    assertThat(errors).isEqualTo(
      listOf("policeForce must be one of AVON_AND_SOMERSET, BEDFORDSHIRE, CHESHIRE, CITY_OF_LONDON, CUMBRIA, DERBYSHIRE, DURHAM, ESSEX, GLOUCESTERSHIRE, GWENT, HAMPSHIRE, HERTFORDSHIRE, HUMBERSIDE, KENT, METROPOLITAN, NORTH_WALES, NOTTINGHAMSHIRE, WEST_MIDLANDS but was 'invalid police force' on row 1."),
    )
  }

  @Test
  fun `it should throw an exception when multiple police forces are present`() {
    val crimeData = listOf(
      createCsvRow(),
      createCsvRow(policeForce = PoliceForce.BEDFORDSHIRE.value),
    ).joinToString("\n").byteInputStream()

    assertThrows<ValidationException> {
      service.parseCsvFile(crimeData)
    }
  }

  @ParameterizedTest(name = "it should parse all valid crime types - {0} -> {1}")
  @MethodSource("crimeTypeValues")
  fun `it should parse all valid crime types`(csvValue: String, enumValue: CrimeType) {
    val crimeData = createCsvRow(crimeTypeId = csvValue).byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(1)
    assertThat(crimes[0].crimeTypeId).isEqualTo(enumValue)
    assertThat(errors).hasSize(0)
  }

  @Test
  fun `it should not parse an invalid crime type`() {
    val crimeData = createCsvRow(crimeTypeId = "invalid crime type").byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(0)
    assertThat(errors).isEqualTo(
      listOf("crimeType must be one of RB, BIAD, AB, BOTD, TOMV, TFP, TFMV but was 'invalid crime type' on row 1."),
    )
  }

  @Test
  fun `it should not parse an invalid crime reference`() {
    val crimeData = createCsvRow(crimeReference = "").byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(0)
    assertThat(errors).isEqualTo(
      listOf("A crime reference must be provided"),
    )
  }

  @Test
  fun `it should not parse an invalid crime date from`() {
    val crimeData = createCsvRow(crimeDateTimeFrom = "").byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(0)
    assertThat(errors).isEqualTo(
      listOf("dateFrom must be a date with format yyyyMMddHHmmss but was '' on row 1."),
    )
  }

  @Test
  fun `it should not parse an invalid crime date to`() {
    val crimeData = createCsvRow(crimeDateTimeTo = "").byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(0)
    assertThat(errors).isEqualTo(
      listOf("dateTo must be a date with format yyyyMMddHHmmss but was '' on row 1."),
    )
  }

  @Test
  fun `it should not parse a crime if dateTo is before dateFrom`() {
    val crimeData = createCsvRow(
      crimeDateTimeFrom = "20250225083000",
      crimeDateTimeTo = "20250125083000",
    ).byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(0)
    assertThat(errors).isEqualTo(
      listOf("Crime date time to must be after crime date time from on row 1."),
    )
  }

  @Test
  fun `it should not parse a crime if crime date windows exceeds 12 hours`() {
    val crimeData = createCsvRow(
      crimeDateTimeFrom = "20250125083000",
      crimeDateTimeTo = "20250325083000",
    ).byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(0)
    assertThat(errors).isEqualTo(
      listOf("Crime date time window must not exceed 12 hours on row 1."),
    )
  }

  @ParameterizedTest(name = "it should parse all valid geodetic datum values - {0} -> {1}")
  @MethodSource("geodeticDatumValues")
  fun `it should parse all valid geodetic datum values`(csvValue: String, enumValue: GeodeticDatum) {
    val crimeData = createCsvRow(datum = csvValue).byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(1)
    assertThat(crimes[0].datum).isEqualTo(enumValue)
    assertThat(errors).hasSize(0)
  }

  @Test
  fun `it should not parse an invalid geodetic datum value`() {
    val crimeData = createCsvRow(datum = "invalid datum").byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(0)
    assertThat(errors).isEqualTo(
      listOf("datum must be one of WGS84, OSGB36 but was 'invalid datum' on row 1."),
    )
  }

  @Test
  fun `it should return many errors if multiple fields are invalid`() {
    val crimeData = createCsvRow(
      policeForce = "invalid police force",
      crimeTypeId = "invalid crime type",
      crimeDateTimeFrom = "",
      crimeDateTimeTo = "",
      datum = "invalid datum",
    ).byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(0)
    assertThat(errors).isEqualTo(
      listOf(
        "policeForce must be one of AVON_AND_SOMERSET, BEDFORDSHIRE, CHESHIRE, CITY_OF_LONDON, CUMBRIA, DERBYSHIRE, DURHAM, ESSEX, GLOUCESTERSHIRE, GWENT, HAMPSHIRE, HERTFORDSHIRE, HUMBERSIDE, KENT, METROPOLITAN, NORTH_WALES, NOTTINGHAMSHIRE, WEST_MIDLANDS but was 'invalid police force' on row 1.",
        "crimeType must be one of RB, BIAD, AB, BOTD, TOMV, TFP, TFMV but was 'invalid crime type' on row 1.",
        "dateFrom must be a date with format yyyyMMddHHmmss but was '' on row 1.",
        "dateTo must be a date with format yyyyMMddHHmmss but was '' on row 1.",
        "datum must be one of WGS84, OSGB36 but was 'invalid datum' on row 1.",
      ),
    )
  }

  @Test
  fun `it should be possible to identity which row the error was on`() {
    val crimeData = listOf(
      createCsvRow(),
      createCsvRow(crimeTypeId = "invalid"),
      createCsvRow(crimeDateTimeFrom = "invalid"),
    ).joinToString("\n").byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)

    assertThat(crimes).hasSize(1)
    assertThat(errors).isEqualTo(
      listOf(
        "crimeType must be one of RB, BIAD, AB, BOTD, TOMV, TFP, TFMV but was 'invalid' on row 2.",
        "dateFrom must be a date with format yyyyMMddHHmmss but was 'invalid' on row 3.",
      ),
    )
  }

  @Test
  fun `it should not parse when multiple location data types are provided`() {
    val crimeData = createCsvRow(easting = "1", northing = "1", latitude = "50", longitude = "1").byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)
    assertThat(crimes).hasSize(0)
    assertThat(errors).isEqualTo(
      listOf(
        "Only one location data type should be provided on row 1.",
        "Only one location data type should be provided on row 1.",
        "Only one location data type should be provided on row 1.",
        "Only one location data type should be provided on row 1.",
      ),
    )
  }

  @ParameterizedTest(name = "easting={0}, northing={1}, lat={2}, long={3}, errorMessage={4}")
  @MethodSource("invalidLocationValues")
  fun `it should not parse invalid location data`(easting: String, northing: String, latitude: String, longitude: String, errorMessage: String) {
    val crimeData = createCsvRow(easting = easting, northing = northing, latitude = latitude, longitude = longitude).byteInputStream()
    val (crimes, errors) = service.parseCsvFile(crimeData)
    assertThat(crimes).hasSize(0)
    assertThat(errors).isEqualTo(
      listOf(errorMessage),
    )
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
    fun geodeticDatumValues() = listOf(
      Arguments.of("WGS84", GeodeticDatum.WGS84),
      Arguments.of("OSGB36", GeodeticDatum.OSGB36),
    )

    @JvmStatic
    fun invalidLocationValues() = listOf(
      Arguments.of("-1", "1", "", "", "easting value '-1.0' outside of valid range on row 1."),
      Arguments.of("600001", "1", "", "", "easting value '600001.0' outside of valid range on row 1."),
      Arguments.of("1", "", "", "", "Dependent location data field must be provided when using easting on row 1."),
      Arguments.of("1", "-1", "", "", "northing value '-1.0' outside of valid range on row 1."),
      Arguments.of("1", "1300001", "", "", "northing value '1300001.0' outside of valid range on row 1."),
      Arguments.of("", "1", "", "", "Dependent location data field must be provided when using northing on row 1."),
      Arguments.of("", "", "62", "1", "latitude value '62.0' outside of valid range on row 1."),
      Arguments.of("", "", "49", "1", "latitude value '49.0' outside of valid range on row 1."),
      Arguments.of("", "", "50", "", "Dependent location data field must be provided when using latitude on row 1."),
      Arguments.of("", "", "50", "-9.0", "longitude value '-9.0' outside of valid range on row 1."),
      Arguments.of("", "", "50", "3", "longitude value '3.0' outside of valid range on row 1."),
      Arguments.of("", "", "", "1", "Dependent location data field must be provided when using longitude on row 1."),
      Arguments.of("", "", "", "", "No location data present on row 1."),
    )
  }
}
