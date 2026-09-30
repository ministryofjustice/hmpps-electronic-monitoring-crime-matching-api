package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.mappers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.geo.CoordinateResolver
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.Wgs84
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeMatchingResultProjection
import java.time.Instant
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class CrimeMatchingResultMapperTest {

  @Mock
  private lateinit var coordinateResolver: CoordinateResolver

  @InjectMocks
  private lateinit var mapper: CrimeMatchingResultMapper

  @Test
  fun `should map valid crime matching result with valid crime type id`() {
    // Given
    val mockProjection = createMockProjection(crimeTypeId = "RB")
    val expectedCoordinates = Wgs84(longitude = -0.1278, latitude = 51.5074)
    whenever(
      coordinateResolver.toWgs84(
        mockProjection.crimeLatitude,
        mockProjection.crimeLongitude,
        mockProjection.crimeEasting,
        mockProjection.crimeNorthing,
      ),
    ).thenReturn(expectedCoordinates)

    // When
    val result = mapper.toDto(mockProjection)

    // Then
    assertThat(result).isNotNull
    assertThat(result.crimeType).isEqualTo("RB")
    assertThat(result.crimeTypeDescription).isEqualTo("Robbery")
    assertThat(result.policeForce).isEqualTo("Test Police Force")
    assertThat(result.crimeRef).isEqualTo("TEST/2026/001")
    assertThat(result.crimeLatitude).isEqualTo(51.5074)
    assertThat(result.crimeLongitude).isEqualTo(-0.1278)
  }

  @Test
  fun `should return empty string for crime description when crime type id is not a valid enum value`() {
    // Given
    val mockProjection = createMockProjection(crimeTypeId = "INVALID")
    val expectedCoordinates = Wgs84(longitude = -0.1278, latitude = 51.5074)
    whenever(
      coordinateResolver.toWgs84(
        mockProjection.crimeLatitude,
        mockProjection.crimeLongitude,
        mockProjection.crimeEasting,
        mockProjection.crimeNorthing,
      ),
    ).thenReturn(expectedCoordinates)

    // When
    val result = mapper.toDto(mockProjection)

    // Then
    assertThat(result.crimeType).isEqualTo("INVALID")
    assertThat(result.crimeTypeDescription).isEmpty()
  }

  @Test
  fun `should map all crime type enum values correctly`() {
    // Given
    val testCases = mapOf(
      "RB" to "Robbery",
      "BIAD" to "Burglary in a dwelling",
      "AB" to "Aggravated Burglary",
      "BOTD" to "Burglary in a building other than a dwelling",
      "TOMV" to "Theft of a vehicle",
      "TFP" to "Theft from the person of another",
      "TFMV" to "Theft from vehicle",
    )

    val expectedCoordinates = Wgs84(longitude = -0.1278, latitude = 51.5074)
    whenever(
      coordinateResolver.toWgs84(
        org.mockito.kotlin.any(),
        org.mockito.kotlin.any(),
        org.mockito.kotlin.any(),
        org.mockito.kotlin.any(),
      ),
    ).thenReturn(expectedCoordinates)

    testCases.forEach { (crimeTypeId, expectedDescription) ->
      // Given
      val mockProjection = createMockProjection(crimeTypeId = crimeTypeId)

      // When
      val result = mapper.toDto(mockProjection)

      // Then
      assertThat(result.crimeType).isEqualTo(crimeTypeId)
      assertThat(result.crimeTypeDescription).isEqualTo(expectedDescription)
    }
  }

  private fun createMockProjection(
    policeForceArea: String = "Test Police Force",
    batchId: String = "BATCH123",
    crimeReference: String = "TEST/2026/001",
    crimeTypeId: String = "RB",
    crimeDateTimeFrom: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    crimeDateTimeTo: Instant = Instant.parse("2026-01-02T00:00:00Z"),
    crimeLatitude: Double? = 51.5074,
    crimeLongitude: Double? = -0.1278,
    crimeEasting: Double? = 530000.0,
    crimeNorthing: Double? = 180000.0,
    crimeText: String = "Test crime",
    address: String = "Test Address",
    dateOfBirth: LocalDateTime? = null,
    deviceId: Long = 1L,
    deviceSerialNumber: String = "SERIAL123",
    deviceName: String = "Device Name",
    deviceModelName: String = "Device Model Name",
    identifier: String = "ID123",
    name: String = "Test Name",
    nomisId: String = "NOMIS123",
    pncRef: String = "PNC123",
  ): CrimeMatchingResultProjection = object : CrimeMatchingResultProjection {
    override val policeForceArea = policeForceArea
    override val batchId = batchId
    override val crimeReference = crimeReference
    override val crimeTypeId = crimeTypeId
    override val crimeDateTimeFrom = crimeDateTimeFrom
    override val crimeDateTimeTo = crimeDateTimeTo
    override val crimeLatitude = crimeLatitude
    override val crimeLongitude = crimeLongitude
    override val crimeEasting = crimeEasting
    override val crimeNorthing = crimeNorthing
    override val crimeText = crimeText
    override val address = address
    override val dateOfBirth = dateOfBirth
    override val deviceId = deviceId
    override val deviceSerialNumber = deviceSerialNumber
    override val deviceName = deviceName
    override val deviceModelName = deviceModelName
    override val identifier = identifier
    override val name = name
    override val nomisId = nomisId
    override val pncRef = pncRef
  }
}
