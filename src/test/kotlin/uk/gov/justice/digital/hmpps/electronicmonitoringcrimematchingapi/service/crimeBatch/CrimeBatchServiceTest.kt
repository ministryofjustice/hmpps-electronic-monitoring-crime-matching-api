package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.MatchingNotificationService
import java.time.LocalDateTime

@ActiveProfiles("test")
class CrimeBatchServiceTest {
  private lateinit var crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository
  private lateinit var crimeBatchRepository: CrimeBatchRepository
  private lateinit var crimeRepository: CrimeRepository
  private lateinit var crimeVersionRepository: CrimeVersionRepository
  private lateinit var service: CrimeBatchService
  private lateinit var matchingNotificationService: MatchingNotificationService

  @BeforeEach
  fun setup() {
    crimeBatchIngestionAttemptRepository = Mockito.mock(CrimeBatchIngestionAttemptRepository::class.java)
    crimeBatchRepository = Mockito.mock(CrimeBatchRepository::class.java)
    crimeRepository = Mockito.mock(CrimeRepository::class.java)
    crimeVersionRepository = Mockito.mock(CrimeVersionRepository::class.java)
    matchingNotificationService = Mockito.mock(MatchingNotificationService::class.java)
    service = CrimeBatchService(crimeBatchIngestionAttemptRepository, crimeBatchRepository, crimeRepository, crimeVersionRepository, matchingNotificationService)
  }

  @Nested
  @DisplayName("createCrimeBatch")
  inner class CreateCrimeBatch {
    @Test
    fun `it should create a crime batch from crime csv records`() {
      val crimeBatchEmailAttachment = Mockito.mock(CrimeBatchEmailAttachment::class.java)

      whenever(crimeRepository.save(any())).thenReturn(
        Crime(
          policeForceArea = PoliceForce.METROPOLITAN,
          crimeReference = "crimeRef",
        ),
      )

      service.createCrimeBatch(
        listOf(
          CrimeRecordDto(
            policeForce = PoliceForce.METROPOLITAN,
            crimeTypeId = CrimeType.AB,
            batchId = "batchId",
            crimeReference = "crimeRef",
            crimeDateTimeFrom = LocalDateTime.of(2025, 1, 25, 8, 30),
            crimeDateTimeTo = LocalDateTime.of(2025, 1, 25, 8, 30),
            easting = null,
            northing = null,
            latitude = 54.732410000000002,
            longitude = -1.38542,
            crimeText = "",
          ),
        ),
        crimeBatchEmailAttachment,
      )
      val crimeCaptor = argumentCaptor<Crime>()
      val crimeBatchCaptor = argumentCaptor<CrimeBatch>()
      val notificationCaptor = argumentCaptor<String>()

      verify(crimeRepository, times(1)).save(crimeCaptor.capture())
      verify(crimeBatchRepository, times(1)).save(crimeBatchCaptor.capture())
      verify(matchingNotificationService, times(1)).publishMatchingRequest(notificationCaptor.capture())

      assertThat(crimeBatchCaptor.allValues.first().crimeBatchEmailAttachment).isEqualTo(crimeBatchEmailAttachment)
      assertThat(crimeBatchCaptor.allValues.first().batchId).isEqualTo("batchId")
      assertThat(crimeBatchCaptor.allValues.first().crimeVersions).isNotEmpty()
      assertThat(crimeBatchCaptor.allValues.first().crimeVersions).hasSize(1)
      assertThat(notificationCaptor.allValues.first()).isEqualTo(crimeBatchCaptor.allValues.first().id.toString())
    }
  }
}
