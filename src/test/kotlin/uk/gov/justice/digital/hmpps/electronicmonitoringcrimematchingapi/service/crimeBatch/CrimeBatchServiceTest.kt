package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.GeodeticDatum
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.MatchingNotificationService
import java.time.LocalDateTime

@ActiveProfiles("test")
class CrimeBatchServiceTest {
  private lateinit var crimeBatchRepository: CrimeBatchRepository
  private lateinit var service: CrimeBatchService
  private lateinit var matchingNotificationService: MatchingNotificationService

  @BeforeEach
  fun setup() {
    crimeBatchRepository = Mockito.mock(CrimeBatchRepository::class.java)
    matchingNotificationService = Mockito.mock(MatchingNotificationService::class.java)
    service = CrimeBatchService(crimeBatchRepository, matchingNotificationService)
  }

  @Nested
  @DisplayName("createCrimeBatch")
  inner class CreateCrimeBatch {
    @Test
    fun `it should create a crime batch from crime csv records`() {
      service.createCrimeBatch(
        listOf(
          CrimeRecordDto(
            policeForce = PoliceForce.METROPOLITAN,
            crimeTypeId = CrimeType.AB,
            crimeReference = "",
            crimeDateTimeFrom = LocalDateTime.of(2025, 1, 25, 8, 30),
            crimeDateTimeTo = LocalDateTime.of(2025, 1, 25, 8, 30),
            easting = null,
            northing = null,
            latitude = 54.732410000000002,
            longitude = -1.38542,
            datum = GeodeticDatum.WGS84,
            crimeText = "",
          ),
        ),
      )
      val crimeBatchCaptor = argumentCaptor<CrimeBatch>()
      val notificationCaptor = argumentCaptor<String>()

      verify(crimeBatchRepository, times(1)).save(crimeBatchCaptor.capture())
      verify(matchingNotificationService, times(1)).publishMatchingRequest(notificationCaptor.capture())

      assertThat(crimeBatchCaptor.allValues.first().policeForce).isEqualTo(PoliceForce.METROPOLITAN)
      assertThat(crimeBatchCaptor.allValues.first().crimes).isNotEmpty()
      assertThat(crimeBatchCaptor.allValues.first().crimes).hasSize(1)
      assertThat(notificationCaptor.allValues.first()).isEqualTo(crimeBatchCaptor.allValues.first().id)
    }
  }
}
