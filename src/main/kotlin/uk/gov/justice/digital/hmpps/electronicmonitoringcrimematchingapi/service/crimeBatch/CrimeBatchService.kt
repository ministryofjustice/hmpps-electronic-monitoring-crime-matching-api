package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.EmailData
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.MatchingNotificationService
import java.util.UUID

@Service
class CrimeBatchService(
  private val crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository,
  private val crimeBatchRepository: CrimeBatchRepository,
  private val crimeRepository: CrimeRepository,
  private val crimeVersionRepository: CrimeVersionRepository,
  private val matchingNotificationService: MatchingNotificationService,
) {

  @Transactional
  fun createCrimeBatch(records: List<CrimeRecordDto>, crimeBatchEmailAttachment: CrimeBatchEmailAttachment) {
    // Create a new batch
    val crimeBatch =  CrimeBatch(
      batchId = records.first().batchId,
      crimeBatchEmailAttachment = crimeBatchEmailAttachment,
    )

    for (record in records) {
      // Check for existing crime else save new crime
      val crime = crimeRepository.findByCrimeReferenceAndPoliceForceArea(record.crimeReference, record.policeForce)
        .orElseGet { crimeRepository.save(Crime(policeForceArea = record.policeForce, crimeReference = record.crimeReference)) }

      // Check for duplicate version
      val crimeVersion = crimeVersionRepository.findByCrimeIdAndCrimeTypeIdAndCrimeDateTimeFromAndCrimeDateTimeToAndEastingAndNorthingAndLatitudeAndLongitudeAndCrimeText(
        crime.id,
            record.crimeTypeId,
            record.crimeDateTimeFrom,
            record.crimeDateTimeTo,
            record.easting,
            record.northing,
            record.latitude,
            record.longitude,
            record.crimeText,
          ) .orElseGet { createCrimeVersion(record, crime) }

      // Add version to batch
      crimeBatch.crimeVersions.add(crimeVersion)
    }

    // Save batch
    crimeBatchRepository.save(crimeBatch)

    matchingNotificationService.publishMatchingRequest(crimeBatch.id)
  }

  fun getCrimeBatch(id: UUID): CrimeBatchDto {
    val crimeBatch = crimeBatchRepository
      .findById(id)
      .orElseThrow {
        EntityNotFoundException("No crime batch found with id: $id")
      }

    return CrimeBatchDto(crimeBatch)
  }

  fun saveCrimeBatchIngestionAttempt(bucketName: String, objectKey: String, emailData: EmailData, recordCount: Int): CrimeBatchEmailAttachment {
    val crimeBatchIngestionAttempt = createCrimeBatchIngestionAttempt(bucketName, objectKey)

    val crimeBatchEmail = createCrimeBatchEmail(emailData, crimeBatchIngestionAttempt)
    crimeBatchIngestionAttempt.crimeBatchEmail = crimeBatchEmail

    val crimeBatchEmailAttachment = createCrimeBatchEmailAttachment(emailData.attachment.name, recordCount, crimeBatchEmail)
    crimeBatchEmail.crimeBatchEmailAttachment = crimeBatchEmailAttachment

    crimeBatchIngestionAttemptRepository.save(crimeBatchIngestionAttempt)

    return crimeBatchEmailAttachment
  }

  private fun createCrimeBatchIngestionAttempt(bucketName: String, objectKey: String): CrimeBatchIngestionAttempt {
    return CrimeBatchIngestionAttempt(
      bucket = bucketName,
      objectName = objectKey,
    )
  }

  private fun createCrimeBatchEmail(emailData: EmailData, crimeBatchIngestionAttempt: CrimeBatchIngestionAttempt): CrimeBatchEmail {
    return CrimeBatchEmail(
      sender = emailData.sender,
      originalSender = emailData.originalSender,
      subject = emailData.subject,
      sentAt = emailData.sentAt,
      crimeBatchIngestionAttempt = crimeBatchIngestionAttempt,
    )
  }

  private fun createCrimeBatchEmailAttachment(fileName: String, recordCount: Int, crimeBatchEmail: CrimeBatchEmail): CrimeBatchEmailAttachment {
    return CrimeBatchEmailAttachment(
      fileName = fileName,
      rowCount = recordCount,
      crimeBatchEmail = crimeBatchEmail,
    )
  }

  private fun createCrimeVersion(record: CrimeRecordDto, crime: Crime): CrimeVersion = CrimeVersion(
    crime = crime,
    crimeTypeId = record.crimeTypeId,
    crimeDateTimeFrom = record.crimeDateTimeFrom,
    crimeDateTimeTo = record.crimeDateTimeTo,
    easting = record.easting,
    northing = record.northing,
    latitude = record.latitude,
    longitude = record.longitude,
    crimeText = record.crimeText,
  )
}
