package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import jakarta.validation.Validation
import jakarta.validation.ValidationException
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helper.createCsvRow
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.caching.CacheEntry
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository

@ActiveProfiles("test")
class CrimeBatchServiceTest {
  private lateinit var crimeBatchRepository: CrimeBatchRepository
  private lateinit var service: CrimeBatchService

  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @BeforeEach
  fun setup() {
    crimeBatchRepository = Mockito.mock(CrimeBatchRepository::class.java)
    service = CrimeBatchService(crimeBatchRepository, validator)
  }

  @Nested
  @DisplayName("IngestCsvData")
  inner class IngestCsvData {
    @Test
    fun `it should parse the csv data and insert valid data into the database`() {
      service.ingestCsvData(createCsvRow().byteInputStream())
      argumentCaptor<CrimeBatch>().apply {
        verify(crimeBatchRepository, times(1)).save(capture())
        assertThat(firstValue.policeForce).isEqualTo("METROPOLITAN")
        assertThat(firstValue.crimes).isNotEmpty()
        assertThat(firstValue.crimes).hasSize(1)
      }
    }

    @Test
    fun `it should continue parsing when an invalid record is found`() {
      val csv = listOf(
        createCsvRow(),
        "",
        createCsvRow(),
      ).joinToString("\n")

      service.ingestCsvData(csv.byteInputStream())
      argumentCaptor<CrimeBatch>().apply {
        verify(crimeBatchRepository, times(1)).save(capture())
        assertThat(firstValue.policeForce).isEqualTo("METROPOLITAN")
        assertThat(firstValue.crimes).isNotEmpty()
        assertThat(firstValue.crimes).hasSize(2)
      }
    }
  }

  @Test
  fun `it should fail to insert the batch if invalid batch data is provided`() {
    assertThrows<ValidationException> {
      service.ingestCsvData(createCsvRow(policeForce = "invalid").byteInputStream())
    }
    argumentCaptor<CacheEntry>().apply {
      verify(crimeBatchRepository, times(0)).save(any())
    }
  }

  @Test
  fun `it should not save a crime when a crime date is invalid`() {
    service.ingestCsvData(createCsvRow(crimeDateTimeFrom = "invalid").byteInputStream())
    argumentCaptor<CrimeBatch>().apply {
      verify(crimeBatchRepository, times(1)).save(capture())
      assertThat(firstValue.crimes).isEmpty()
    }
  }

  @Test
  fun `it should not save a crime when the crime date range is invalid`() {
    service.ingestCsvData(createCsvRow(crimeDateTimeFrom = "20250225083000").byteInputStream())
    argumentCaptor<CrimeBatch>().apply {
      verify(crimeBatchRepository, times(1)).save(capture())
      assertThat(firstValue.crimes).isEmpty()
    }
  }

  @Test
  fun `it should not save a crime when the crime type is invalid`() {
    service.ingestCsvData(createCsvRow(crimeTypeId = "invalid").byteInputStream())
    argumentCaptor<CrimeBatch>().apply {
      verify(crimeBatchRepository, times(1)).save(capture())
      assertThat(firstValue.crimes).isEmpty()
    }
  }

  @Test
  fun `it should not save a crime when the crime reference is invalid`() {
    service.ingestCsvData(createCsvRow(crimeReference = "").byteInputStream())
    argumentCaptor<CrimeBatch>().apply {
      verify(crimeBatchRepository, times(1)).save(capture())
      assertThat(firstValue.crimes).isEmpty()
    }
  }
}
