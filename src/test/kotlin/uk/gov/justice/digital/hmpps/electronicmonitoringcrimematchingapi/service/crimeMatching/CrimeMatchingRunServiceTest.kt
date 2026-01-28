package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeMatching

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeMatchingResultDeviceWearerDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeMatchingResultDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeMatchingResultPositionDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeMatchingRunDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingRun
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeMatchingStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeMatchingTriggerType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingRunRepository
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

@ActiveProfiles("test")
class CrimeMatchingRunServiceTest {
  private lateinit var crimeMatchingRunRepository: CrimeMatchingRunRepository
  private lateinit var crimeBatchRepository: CrimeBatchRepository
  private lateinit var crimeVersionRepository: CrimeVersionRepository
  private lateinit var service: CrimeMatchingRunService

  @BeforeEach
  fun setup() {
    crimeMatchingRunRepository = Mockito.mock(CrimeMatchingRunRepository::class.java)
    crimeBatchRepository = Mockito.mock(CrimeBatchRepository::class.java)
    crimeVersionRepository = Mockito.mock(CrimeVersionRepository::class.java)
    service = CrimeMatchingRunService(crimeMatchingRunRepository, crimeBatchRepository, crimeVersionRepository)
  }

  @Nested
  @DisplayName("createCrimeMatchingRun")
  inner class CreateCrimeMatchingRun {

    @Test
    fun `it should throw when matchingEnded is before matchingStarted`() {
      val dto = CrimeMatchingRunDto(
        crimeBatchId = UUID.randomUUID(),
        algorithmVersion = "e83c5163316f89bfbde7d9ab23ca2e25604af290",
        triggerType = CrimeMatchingTriggerType.AUTO,
        status = CrimeMatchingStatus.SUCCESS,
        matchingStarted = LocalDateTime.of(2026, 1, 16, 8, 31, 10),
        matchingEnded = LocalDateTime.of(2026, 1, 16, 8, 30, 0),
        results = emptyList(),
      )

      assertThatThrownBy { service.createCrimeMatchingRun(dto) }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("matchingEnded must be the same as or after matchingStarted")
    }

    @Test
    fun `it should throw a not found when the crime batch does not exist`() {
      val missingBatchId = UUID.randomUUID()

      whenever(crimeBatchRepository.findById(missingBatchId)).thenReturn(Optional.empty())

      val dto = CrimeMatchingRunDto(
        crimeBatchId = missingBatchId,
        algorithmVersion = "e83c5163316f89bfbde7d9ab23ca2e25604af290",
        triggerType = CrimeMatchingTriggerType.AUTO,
        status = CrimeMatchingStatus.SUCCESS,
        matchingStarted = LocalDateTime.of(2026, 1, 16, 8, 30, 0),
        matchingEnded = LocalDateTime.of(2026, 1, 16, 8, 31, 0),
        results = emptyList(),
      )

      assertThatThrownBy { service.createCrimeMatchingRun(dto) }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("No crime batch found with id: $missingBatchId")
    }

    @Test
    fun `it should throw a not found when a referenced crime version does not exist`() {
      val batchId = UUID.randomUUID()
      val missingVersionId = UUID.randomUUID()

      val crimeBatch = Mockito.mock(CrimeBatch::class.java)

      whenever(crimeBatchRepository.findById(batchId)).thenReturn(Optional.of(crimeBatch))
      whenever(crimeVersionRepository.findById(missingVersionId)).thenReturn(Optional.empty())

      val dto = CrimeMatchingRunDto(
        crimeBatchId = batchId,
        algorithmVersion = "e83c5163316f89bfbde7d9ab23ca2e25604af290",
        triggerType = CrimeMatchingTriggerType.AUTO,
        status = CrimeMatchingStatus.SUCCESS,
        matchingStarted = LocalDateTime.of(2026, 1, 16, 8, 30, 0),
        matchingEnded = LocalDateTime.of(2026, 1, 16, 8, 31, 0),
        results = listOf(CrimeMatchingResultDto(crimeVersionId = missingVersionId, deviceWearers = emptyList())),
      )

      assertThatThrownBy { service.createCrimeMatchingRun(dto) }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("No crime version found with id: $missingVersionId")
    }

    @Test
    fun `it should create a run with correct top-level fields and save it`() {
      val batchId = UUID.randomUUID()
      val crimeBatch = Mockito.mock(CrimeBatch::class.java)

      whenever(crimeBatchRepository.findById(batchId)).thenReturn(Optional.of(crimeBatch))
      whenever(crimeMatchingRunRepository.save(any())).thenAnswer { it.arguments[0] as CrimeMatchingRun }

      val dto = CrimeMatchingRunDto(
        crimeBatchId = batchId,
        algorithmVersion = "e83c5163316f89bfbde7d9ab23ca2e25604af290",
        triggerType = CrimeMatchingTriggerType.AUTO,
        status = CrimeMatchingStatus.SUCCESS,
        matchingStarted = LocalDateTime.of(2026, 1, 16, 8, 30, 0),
        matchingEnded = LocalDateTime.of(2026, 1, 16, 8, 31, 0),
        results = emptyList(),
      )

      service.createCrimeMatchingRun(dto)

      val crimeMatchingRunCaptor = argumentCaptor<CrimeMatchingRun>()
      verify(crimeMatchingRunRepository, times(1)).save(crimeMatchingRunCaptor.capture())

      val savedRun = crimeMatchingRunCaptor.allValues.first()
      assertThat(savedRun.crimeBatch).isEqualTo(crimeBatch)
      assertThat(savedRun.algorithmVersion).isEqualTo("e83c5163316f89bfbde7d9ab23ca2e25604af290")
      assertThat(savedRun.triggerType).isEqualTo(CrimeMatchingTriggerType.AUTO)
      assertThat(savedRun.status).isEqualTo(CrimeMatchingStatus.SUCCESS)
      assertThat(savedRun.matchingStarted).isEqualTo(LocalDateTime.of(2026, 1, 16, 8, 30, 0))
      assertThat(savedRun.matchingEnded).isEqualTo(LocalDateTime.of(2026, 1, 16, 8, 31, 0))
      assertThat(savedRun.results).isEmpty()
    }

    @Test
    fun `it should map and persist nested results device wearers and positions`() {
      val batchId = UUID.randomUUID()
      val versionId = UUID.randomUUID()

      val crimeBatch = Mockito.mock(CrimeBatch::class.java)
      val crimeVersion = Mockito.mock(CrimeVersion::class.java)

      whenever(crimeBatchRepository.findById(batchId)).thenReturn(Optional.of(crimeBatch))
      whenever(crimeVersionRepository.findById(versionId)).thenReturn(Optional.of(crimeVersion))
      whenever(crimeMatchingRunRepository.save(any())).thenAnswer { it.arguments[0] as CrimeMatchingRun }

      val dto = CrimeMatchingRunDto(
        crimeBatchId = batchId,
        algorithmVersion = "e83c5163316f89bfbde7d9ab23ca2e25604af290",
        triggerType = CrimeMatchingTriggerType.AUTO,
        status = CrimeMatchingStatus.SUCCESS,
        matchingStarted = LocalDateTime.of(2026, 1, 16, 8, 30, 0),
        matchingEnded = LocalDateTime.of(2026, 1, 16, 8, 31, 0),
        results = listOf(
          CrimeMatchingResultDto(
            crimeVersionId = versionId,
            deviceWearers = listOf(
              CrimeMatchingResultDeviceWearerDto(
                deviceId = 604008982,
                name = "Richard Gibbons",
                nomisId = "A5128CZ",
                positions = listOf(
                  CrimeMatchingResultPositionDto(
                    latitude = 51.574865,
                    longitude = 0.060977,
                    capturedDateTime = LocalDateTime.of(2026, 1, 16, 8, 12, 0),
                    sequenceLabel = "A1",
                    confidenceCircle = 30,
                  ),
                  CrimeMatchingResultPositionDto(
                    latitude = 51.574153,
                    longitude = 0.058536,
                    capturedDateTime = LocalDateTime.of(2026, 1, 16, 8, 12, 0),
                    sequenceLabel = "A2",
                    confidenceCircle = 52,
                  ),
                ),
              ),
            ),
          ),
        ),
      )

      service.createCrimeMatchingRun(dto)

      val crimeMatchingRunCaptor = argumentCaptor<CrimeMatchingRun>()
      verify(crimeMatchingRunRepository, times(1)).save(crimeMatchingRunCaptor.capture())

      val savedRun = crimeMatchingRunCaptor.firstValue
      assertThat(savedRun.results).hasSize(1)

      val savedResult = savedRun.results.first()
      assertThat(savedResult.crimeVersion).isEqualTo(crimeVersion)
      assertThat(savedResult.deviceWearers).hasSize(1)

      val savedWearer = savedResult.deviceWearers.first()
      assertThat(savedWearer.deviceId).isEqualTo(604008982)
      assertThat(savedWearer.name).isEqualTo("Richard Gibbons")
      assertThat(savedWearer.nomisId).isEqualTo("A5128CZ")
      assertThat(savedWearer.positions).hasSize(2)

      val firstPos = savedWearer.positions.first()
      assertThat(firstPos.sequenceLabel).isEqualTo("A1")
      assertThat(firstPos.latitude).isEqualTo(51.574865)
      assertThat(firstPos.longitude).isEqualTo(0.060977)
      assertThat(firstPos.confidenceCircle).isEqualTo(30)
    }
  }
}
