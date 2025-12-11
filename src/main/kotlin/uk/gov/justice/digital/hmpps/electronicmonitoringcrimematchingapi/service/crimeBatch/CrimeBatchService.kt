package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordDto
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
import java.time.LocalDateTime
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
      batchId = "batchId-" + UUID.randomUUID(),
      crimeBatchEmailAttachment = crimeBatchEmailAttachment,
    )

    // Parse crime records
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

  fun saveCrimeBatchDetails(bucketName: String, objectKey: String): CrimeBatchEmailAttachment {
    val crimeBatchIngestionAttempt = CrimeBatchIngestionAttempt(
      bucket = bucketName,
      objectName = objectKey,
    )

    val crimeBatchEmail = CrimeBatchEmail(
      sender = "",
      originalSender = "",
      subject = "",
      sentAt = LocalDateTime.now(),
      crimeBatchIngestionAttempt = crimeBatchIngestionAttempt,
    )
    crimeBatchIngestionAttempt.crimeBatchEmail = crimeBatchEmail

    val crimeBatchEmailAttachment = CrimeBatchEmailAttachment(
      fileName = "",
      rowCount = 1,
      crimeBatchEmail = crimeBatchEmail,
    )
    crimeBatchEmail.crimeBatchEmailAttachments.add(crimeBatchEmailAttachment)

    crimeBatchIngestionAttemptRepository.save(crimeBatchIngestionAttempt)

    return crimeBatchEmailAttachment
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
