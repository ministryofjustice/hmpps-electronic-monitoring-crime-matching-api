package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository

@ActiveProfiles("test")
class CrimeBatchServiceTest {
  private lateinit var crimeBatchRepository: CrimeBatchRepository
  private lateinit var crimeRepository: CrimeRepository
  private lateinit var service: CrimeBatchService

  @BeforeEach
  fun setup() {
    crimeBatchRepository = Mockito.mock(CrimeBatchRepository::class.java)
    crimeRepository = Mockito.mock(CrimeRepository::class.java)
    service = CrimeBatchService(crimeBatchRepository, crimeRepository)
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
      }

      argumentCaptor<List<Crime>>().apply {
        verify(crimeRepository, times(1)).saveAll(capture())
        assertThat(firstValue).hasSize(1)
        assertThat(firstValue[0].crimeReference).isEqualTo("CRI00000001")
      }

      verify(crimeRepository, times(1)).saveAll(anyList())
    }

    @Test
    fun `it should throw a ValidationException when CSV is empty`() {
      assertThrows<ValidationException> {
        service.ingestCsvData("".byteInputStream())
      }
    }

    @Test
    fun `it should continue parsing when an invalid record is found`() {
      val csv = """
        policeForce,TOMV,Theft of a motor vehicle,batchId,CRI00000001,2.02501E+13,2.02501E+13,,,54.732410000000002,-1.38542,WGS84,
        ,TOMV,Theft of a motor vehicle,batchId,CRI00000002,2.02501E+13,2.02501E+13,,,54.732410000000002,-1.38542,WGS84,
        policeForce,TOMV,Theft of a motor vehicle,batchId,CRI00000003,2.02501E+13,2.02501E+13,,,54.732410000000002,-1.38542,WGS84,
      """.trimIndent()

      service.ingestCsvData(csv.byteInputStream())
      verify(crimeBatchRepository, times(1)).save(any())
      verify(crimeRepository, times(1)).saveAll(anyList())
    }

    @Test
    fun `it should insert multiple batches when the batch size limit is reached`() {
      val csv = (1..150).joinToString("\n") {
        "policeForce, TOMV, Theft of a motor vehicle, batchId, CRI00000001, 2.02501E+13, 2.02501E+13,,, 54.732410000000002, -1.38542, WGS84,"
      }
      service.ingestCsvData(csv.byteInputStream())
      verify(crimeBatchRepository, times(1)).save(any())
      verify(crimeRepository, times(2)).saveAll(anyList())
    }
  }
}
