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
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository

@ActiveProfiles("test")
class CrimeBatchServiceTest {
  private lateinit var crimeBatchRepository: CrimeBatchRepository
  private lateinit var service: CrimeBatchService

  @BeforeEach
  fun setup() {
    crimeBatchRepository = Mockito.mock(CrimeBatchRepository::class.java)
    service = CrimeBatchService(crimeBatchRepository)
  }

  @Nested
  @DisplayName("IngestCsvData")
  inner class IngestCsvData {
    @Test
    fun `it should parse the csv data and insert valid data into the database`() {
      val csv = """
        policeForce,TOMV,Theft of a motor vehicle,batchId,CRI00000001,2.02501E+13,2.02501E+13,,,54.732410000000002,-1.38542,WGS84,
      """.trimIndent()

      service.ingestCsvData(csv.byteInputStream())
      argumentCaptor<CrimeBatch>().apply {
        verify(crimeBatchRepository, times(1)).save(capture())
        assertThat(firstValue.policeForce).isEqualTo("policeForce")
        assertThat(firstValue.crimes).isNotEmpty()
        assertThat(firstValue.crimes).hasSize(1)
      }
    }

    @Test
    fun `it should continue parsing when an invalid record is found`() {
      val csv = """
        policeForce,TOMV,Theft of a motor vehicle,batchId,CRI00000001,2.02501E+13,2.02501E+13,,,54.732410000000002,-1.38542,WGS84,
        ,
        policeForce,TOMV,Theft of a motor vehicle,batchId,CRI00000003,2.02501E+13,2.02501E+13,,,54.732410000000002,-1.38542,WGS84,
      """.trimIndent()

      service.ingestCsvData(csv.byteInputStream())
      argumentCaptor<CrimeBatch>().apply {
        verify(crimeBatchRepository, times(1)).save(capture())
        assertThat(firstValue.policeForce).isEqualTo("policeForce")
        assertThat(firstValue.crimes).isNotEmpty()
        assertThat(firstValue.crimes).hasSize(2)
      }
    }
  }
}
