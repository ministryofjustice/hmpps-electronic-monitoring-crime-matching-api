package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.EmailIngestionOutcome
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchEmailIngestionService
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch.CrimeBatchService
import java.time.Instant
import java.util.Date

class EmailIngestionFinalisationServiceTest {
  private lateinit var crimeBatchEmailIngestionService: CrimeBatchEmailIngestionService
  private lateinit var crimeBatchService: CrimeBatchService
  private lateinit var service: EmailIngestionFinalisationService

  @BeforeEach
  fun setup() {
    crimeBatchEmailIngestionService = Mockito.mock(CrimeBatchEmailIngestionService::class.java)
    crimeBatchService = Mockito.mock(CrimeBatchService::class.java)
    service = EmailIngestionFinalisationService(crimeBatchEmailIngestionService, crimeBatchService)
  }

  @Test
  fun `it should persist ingestion and create crime batch for successful outcomes`() {
    val attempt = CrimeBatchIngestionAttempt(bucket = "emails", objectName = "object")
    val email = CrimeBatchEmail(
      crimeBatchIngestionAttempt = attempt,
      sender = "sender@test.local",
      originalSender = "sender@test.local",
      subject = "Crime Mapping Request",
      sentAt = Date.from(Instant.now()),
    )
    val attachment = CrimeBatchEmailAttachment(
      crimeBatchEmail = email,
      fileName = "crime.csv",
      rowCount = 1,
    )
    email.crimeBatchEmailAttachments += attachment
    attempt.crimeBatchEmail = email

    val record = CrimeRecordRequest(
      policeForce = PoliceForce.METROPOLITAN,
      crimeTypeId = CrimeType.TOMV,
      batchId = "MPS20260123",
      crimeReference = "CRI000001",
      crimeDateTimeFrom = Instant.parse("2025-01-25T08:30:00Z"),
      crimeDateTimeTo = Instant.parse("2025-01-25T09:30:00Z"),
      easting = null,
      northing = null,
      latitude = 51.5072,
      longitude = -0.1276,
      crimeText = "Theft",
    )

    val preparation = EmailIngestionPreparation(
      crimeBatchIngestionAttempt = attempt,
      ingestionOutcome = EmailIngestionOutcome(
        batchId = record.batchId,
        policeForce = record.policeForce.label,
        records = listOf(record),
        emailData = EmailData(
          sender = "sender@test.local",
          originalSender = "sender@test.local",
          subject = "Crime Mapping Request",
          sentAt = Date.from(Instant.now()),
          attachments = emptyList(),
        ),
        ingestionStatus = IngestionStatus.SUCCESSFUL,
      ),
    )

    val crimeBatch = CrimeBatch(
      batchId = record.batchId,
      crimeBatchEmailAttachment = attachment,
    )

    whenever(crimeBatchEmailIngestionService.saveCrimeBatchIngestionAttempt(attempt)).thenReturn(attempt)
    whenever(crimeBatchService.createCrimeBatch(listOf(record), attachment)).thenReturn(crimeBatch)

    val outcome = service.persistIngestion(preparation)

    verify(crimeBatchEmailIngestionService, times(1)).saveCrimeBatchIngestionAttempt(attempt)
    verify(crimeBatchService, times(1)).createCrimeBatch(listOf(record), attachment)
    assertThat(outcome.batchId).isEqualTo(crimeBatch.batchId)
    assertThat(outcome.crimeBatchId).isEqualTo(crimeBatch.id.toString())
  }

  @Test
  fun `it should persist ingestion without creating a batch for failed outcomes`() {
    val attempt = CrimeBatchIngestionAttempt(bucket = "emails", objectName = "object")
    val preparation = EmailIngestionPreparation(
      crimeBatchIngestionAttempt = attempt,
      ingestionOutcome = EmailIngestionOutcome(
        emailData = EmailData(
          sender = "sender@test.local",
          originalSender = "sender@test.local",
          subject = "Crime Mapping Request",
          sentAt = Date.from(Instant.now()),
          attachments = emptyList(),
        ),
        ingestionStatus = IngestionStatus.FAILED,
      ),
    )

    whenever(crimeBatchEmailIngestionService.saveCrimeBatchIngestionAttempt(attempt)).thenReturn(attempt)

    val outcome = service.persistIngestion(preparation)

    verify(crimeBatchEmailIngestionService, times(1)).saveCrimeBatchIngestionAttempt(attempt)
    verify(crimeBatchService, never()).createCrimeBatch(any(), any())
    assertThat(outcome.ingestionStatus).isEqualTo(IngestionStatus.FAILED)
  }
}
