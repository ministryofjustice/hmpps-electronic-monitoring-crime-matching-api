package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.proximityAlert

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeVersionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeVersionProjection
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@ActiveProfiles("test")
class CrimeVersionServiceTest {
  private lateinit var crimeVersionRepository: CrimeVersionRepository
  private lateinit var service: CrimeVersionService

  @BeforeEach
  fun setup() {
    crimeVersionRepository = Mockito.mock(CrimeVersionRepository::class.java)
    service = CrimeVersionService(
      crimeVersionRepository,
    )
  }

  @Nested
  @DisplayName("GetCrimeVersion")
  inner class GetCrimeVersion {
    @Test
    fun `it should get a crime version`() {
      val mockProjection = mock<CrimeVersionProjection> {
        on { crimeVersionId } doReturn UUID.randomUUID()
        on { crimeReference } doReturn "crimeRef"
        on { crimeType } doReturn CrimeType.AB
        on { crimeDateTimeFrom } doReturn LocalDateTime.now().toInstant(ZoneOffset.UTC)
        on { crimeDateTimeTo } doReturn LocalDateTime.now().toInstant(ZoneOffset.UTC)
        on { crimeText } doReturn "crimeText"
        on { matchingResultId } doReturn "4321"
        on { deviceWearerId } doReturn "1234"
        on { name } doReturn "name"
        on { nomisId } doReturn "nomis"
        on { latitude } doReturn 10.0
        on { longitude } doReturn 10.0
        on { sequenceLabel } doReturn "A1"
        on { confidence } doReturn 10
        on { capturedDateTime } doReturn LocalDateTime.now()
      }

      whenever(crimeVersionRepository.findCrimeVersionMatchingResult(any())).thenReturn(listOf(mockProjection))

      val res = service.getCrimeVersion(UUID.randomUUID())
      assertThat(res).isInstanceOf(CrimeVersionResponse::class.java)
      assertThat(res.matching?.deviceWearers?.size).isEqualTo(1)
      assertThat(res.matching?.deviceWearers?.first()?.positions?.size).isEqualTo(1)
    }
  }
}
