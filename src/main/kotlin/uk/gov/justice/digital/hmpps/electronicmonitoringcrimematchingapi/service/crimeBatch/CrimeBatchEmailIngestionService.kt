package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.IngestionAttemptSummary
import java.time.LocalDateTime

@Service
class CrimeBatchEmailIngestionService(
  private val crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository,
) {
  fun saveCrimeBatchIngestionAttempt(crimeBatchIngestionAttempt: CrimeBatchIngestionAttempt): CrimeBatchIngestionAttempt = crimeBatchIngestionAttemptRepository.save(crimeBatchIngestionAttempt)

  fun createCrimeBatchIngestionAttempt(bucketName: String, objectKey: String): CrimeBatchIngestionAttempt = CrimeBatchIngestionAttempt(
    bucket = bucketName,
    objectName = objectKey,
  )

  fun createCrimeBatchEmail(emailData: EmailData, crimeBatchIngestionAttempt: CrimeBatchIngestionAttempt): CrimeBatchEmail = CrimeBatchEmail(
    sender = emailData.sender,
    originalSender = emailData.originalSender,
    subject = emailData.subject,
    sentAt = emailData.sentAt,
    crimeBatchIngestionAttempt = crimeBatchIngestionAttempt,
  )

  fun createCrimeBatchEmailAttachment(fileName: String, recordCount: Int, crimeBatchEmail: CrimeBatchEmail): CrimeBatchEmailAttachment = CrimeBatchEmailAttachment(
    fileName = fileName,
    rowCount = recordCount,
    crimeBatchEmail = crimeBatchEmail,
  )

  fun getCrimeBatchIngestionAttempts(
    batchId: String?,
    policeForceArea: String?,
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    pageable: Pageable,
  ): Page<IngestionAttemptSummary> = crimeBatchIngestionAttemptRepository.findByBatchId(
    batchId,
    policeForceArea,
    fromDate,
    toDate,
    pageable,
  )
}
