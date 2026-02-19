package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository

@ActiveProfiles("test")
class CrimeBatchEmailIngestionServiceTest {
  private lateinit var crimeBatchEmailIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository
  private lateinit var service: CrimeBatchEmailIngestionService

  @BeforeEach
  fun setup() {
    crimeBatchEmailIngestionAttemptRepository = Mockito.mock(CrimeBatchIngestionAttemptRepository::class.java)
    service = CrimeBatchEmailIngestionService(crimeBatchEmailIngestionAttemptRepository)
  }

  @Nested
  @DisplayName("getCrimeBatchIngestionAttempts")
  inner class GetCrimeBatchIngestionAttempts {
    @Test
    fun `it should get a list of crime batch ingestion attempts`() {
//      whenever(crimeBatchEmailIngestionAttemptRepository.findByBatchId(
//        any(),
//        any(),
//        any(),
//        any(),
//        any(),
//      )).thenReturn()
    }
  }

}