package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

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
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.MatchingNotification
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.IngestionStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.MatchingNotificationService
import java.time.Instant
import java.util.Date

class CrimeBatchEmailIngestionServiceTest {
  private lateinit var crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository
  private lateinit var crimeBatchService: CrimeBatchService
  private lateinit var matchingNotificationService: MatchingNotificationService
  private lateinit var service: CrimeBatchEmailIngestionService

  @BeforeEach
  fun setup() {
    crimeBatchIngestionAttemptRepository = Mockito.mock(CrimeBatchIngestionAttemptRepository::class.java)
    crimeBatchService = Mockito.mock(CrimeBatchService::class.java)
    matchingNotificationService = Mockito.mock(MatchingNotificationService::class.java)
    service = CrimeBatchEmailIngestionService(crimeBatchIngestionAttemptRepository, crimeBatchService, matchingNotificationService)
  }

  @Test
  fun `it should persist ingestion attempt and create crime batch for SUCCESSFUL outcomes`() {
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

    val ingestionOutcome = EmailIngestionOutcome(
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
    )

    val crimeBatch = CrimeBatch(
      batchId = record.batchId,
      crimeBatchEmailAttachment = attachment,
    )

    whenever(crimeBatchIngestionAttemptRepository.save(attempt)).thenReturn(attempt)
    whenever(crimeBatchService.createCrimeBatch(listOf(record), attachment)).thenReturn(crimeBatch)

    val outcome = service.persistIngestion(attempt, ingestionOutcome)

    verify(crimeBatchIngestionAttemptRepository, times(1)).save(attempt)
    verify(crimeBatchService, times(1)).createCrimeBatch(listOf(record), attachment)
    assertThat(outcome.batchId).isEqualTo(crimeBatch.batchId)
    assertThat(outcome.crimeBatchId).isEqualTo(crimeBatch.id.toString())
  }

  @Test
  fun `it should save the publish matching outbox as PENDING_OR_UNCONFIRMED for SUCCESSFUL outcomes`() {
    val attempt = givenIngestionAttemptWithAttachment()
    val record = givenCrimeRecordRequest(batchId = "MPS20260123")
    val ingestionOutcome = givenIngestionOutcome(record = record, ingestionStatus = IngestionStatus.SUCCESSFUL)
    val crimeBatch = CrimeBatch(
      batchId = record.batchId,
      crimeBatchEmailAttachment = attempt.crimeBatchEmail!!.crimeBatchEmailAttachments.first(),
    )

    whenever(crimeBatchIngestionAttemptRepository.save(attempt)).thenReturn(attempt)
    whenever(crimeBatchService.createCrimeBatch(listOf(record), attempt.crimeBatchEmail!!.crimeBatchEmailAttachments.first())).thenReturn(crimeBatch)

    service.persistIngestion(attempt, ingestionOutcome)

    verify(matchingNotificationService, times(1)).savePublishMatchingRequest(any<MatchingNotification>())
  }

  @Test
  fun `it should save the publish matching outbox as PENDING_OR_UNCONFIRMED for PARTIAL outcomes`() {
    val attempt = givenIngestionAttemptWithAttachment()
    val record = givenCrimeRecordRequest(batchId = "MPS20260124")
    val ingestionOutcome = givenIngestionOutcome(record = record, ingestionStatus = IngestionStatus.PARTIAL)
    val crimeBatch = CrimeBatch(
      batchId = record.batchId,
      crimeBatchEmailAttachment = attempt.crimeBatchEmail!!.crimeBatchEmailAttachments.first(),
    )

    whenever(crimeBatchIngestionAttemptRepository.save(attempt)).thenReturn(attempt)
    whenever(crimeBatchService.createCrimeBatch(listOf(record), attempt.crimeBatchEmail!!.crimeBatchEmailAttachments.first())).thenReturn(crimeBatch)

    service.persistIngestion(attempt, ingestionOutcome)

    verify(matchingNotificationService, times(1)).savePublishMatchingRequest(any<MatchingNotification>())
  }

  @Test
  fun `it should persist ingestion attempt without creating a batch for FAILED outcomes`() {
    val attempt = CrimeBatchIngestionAttempt(bucket = "emails", objectName = "object")
    val ingestionOutcome = EmailIngestionOutcome(
      emailData = EmailData(
        sender = "sender@test.local",
        originalSender = "sender@test.local",
        subject = "Crime Mapping Request",
        sentAt = Date.from(Instant.now()),
        attachments = emptyList(),
      ),
      ingestionStatus = IngestionStatus.FAILED,
    )

    whenever(crimeBatchIngestionAttemptRepository.save(attempt)).thenReturn(attempt)

    val outcome = service.persistIngestion(attempt, ingestionOutcome)

    verify(crimeBatchIngestionAttemptRepository, times(1)).save(attempt)
    verify(crimeBatchService, never()).createCrimeBatch(any(), any())
    assertThat(outcome.ingestionStatus).isEqualTo(IngestionStatus.FAILED)
  }

  @Test
  fun `it should not save a row to the publish matching outbox for FAILED outcomes`() {
    val attempt = CrimeBatchIngestionAttempt(bucket = "emails", objectName = "object")
    val ingestionOutcome = EmailIngestionOutcome(
      emailData = EmailData(
        sender = "sender@test.local",
        originalSender = "sender@test.local",
        subject = "Crime Mapping Request",
        sentAt = Date.from(Instant.now()),
        attachments = emptyList(),
      ),
      ingestionStatus = IngestionStatus.FAILED,
    )

    whenever(crimeBatchIngestionAttemptRepository.save(attempt)).thenReturn(attempt)

    service.persistIngestion(attempt, ingestionOutcome)

    verify(matchingNotificationService, times(0)).savePublishMatchingRequest(any())
  }

  private fun givenIngestionAttemptWithAttachment(): CrimeBatchIngestionAttempt {
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
    return attempt
  }

  private fun givenCrimeRecordRequest(batchId: String) = CrimeRecordRequest(
    policeForce = PoliceForce.METROPOLITAN,
    crimeTypeId = CrimeType.TOMV,
    batchId = batchId,
    crimeReference = "CRI000001",
    crimeDateTimeFrom = Instant.parse("2025-01-25T08:30:00Z"),
    crimeDateTimeTo = Instant.parse("2025-01-25T09:30:00Z"),
    easting = null,
    northing = null,
    latitude = 51.5072,
    longitude = -0.1276,
    crimeText = "Theft",
  )

  private fun givenIngestionOutcome(record: CrimeRecordRequest, ingestionStatus: IngestionStatus) = EmailIngestionOutcome(
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
    ingestionStatus = ingestionStatus,
  )
}
