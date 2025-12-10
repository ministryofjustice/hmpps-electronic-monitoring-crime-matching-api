package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeBatchDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchCrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchCrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchEmailAttachmentRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchEmailRepository
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
  private val crimeBatchEmailRepository: CrimeBatchEmailRepository,
  private val crimeBatchEmailAttachmentRepository: CrimeBatchEmailAttachmentRepository,
  private val crimeBatchRepository: CrimeBatchRepository,
  private val crimeRepository: CrimeRepository,
  private val crimeVersionRepository: CrimeVersionRepository,
  private val crimeBatchCrimeVersionRepository: CrimeBatchCrimeVersionRepository,
  private val matchingNotificationService: MatchingNotificationService,
) {

  @Transactional
  fun createCrimeBatch(records: List<CrimeRecordDto>, crimeBatchEmailAttachment: CrimeBatchEmailAttachment) {
    // CrimeBatch
    val crimeBatch = crimeBatchRepository.save(CrimeBatch(
      batchId = "batchId-" + UUID.randomUUID(),
      crimeBatchEmailAttachment = crimeBatchEmailAttachment
    ))

    for (record in records) {
      // Check for existing crime
      val crime = crimeRepository.findByCrimeReferenceAndPoliceForceArea(record.crimeReference, record.policeForce)
        .orElseGet { Crime(policeForceArea = record.policeForce, crimeReference = record.crimeReference) }

      // Check for existing crime versions
      if (crime.crimeVersions.isNotEmpty()) {
        // if crime has versions check for duplicates, if duplicate found continue
        if (crimeVersionRepository.existsByCrimeTypeIdAndCrimeDateTimeFromAndCrimeDateTimeToAndEastingAndNorthingAndLatitudeAndLongitudeAndCrimeText(
          record.crimeTypeId,
          record.crimeDateTimeFrom,
          record.crimeDateTimeTo,
          record.easting,
          record.northing,
          record.latitude,
          record.longitude,
          record.crimeText,
        )) continue
      }

      // Save crime with new version
      val crimeVersion = createCrimeVersion(record, crime)
      crime.crimeVersions.add(crimeVersion)

      crimeRepository.save(crime)

      val crimeBatchCrimeVersion = CrimeBatchCrimeVersion(
        crimeVersionId = crimeVersion.id, batchId = crimeBatch.batchId
      )

      // Save crime batch version
      crimeBatchCrimeVersionRepository.save(crimeBatchCrimeVersion)
    }

    matchingNotificationService.publishMatchingRequest(crimeBatch.id)
  }

  fun createCrimeBatchIngestionAttempt(bucketName: String, objectName: String): CrimeBatchIngestionAttempt {
    return crimeBatchIngestionAttemptRepository.save(CrimeBatchIngestionAttempt(bucket = bucketName, objectName = objectName))
  }

  fun createCrimeBatchEmail(crimeBatchIngestionAttempt: CrimeBatchIngestionAttempt): CrimeBatchEmailAttachment {
    // CrimeBatchEmail
    val crimeBatchEmailEntity = CrimeBatchEmail(
      crimeBatchIngestionAttempt = crimeBatchIngestionAttempt,
      sender = "",
      originalSender = "",
      subject = "",
      sentAt = LocalDateTime.now(),
    )

    val crimeBatchEmail = crimeBatchEmailRepository.save(crimeBatchEmailEntity)

    // CrimeBatchEmailAttachment
    val crimeBatchEmailAttachment = crimeBatchEmailAttachmentRepository.save(CrimeBatchEmailAttachment(
      crimeBatchEmail = crimeBatchEmail,
      fileName = "",
      rowCount = 1,
    ))

    return crimeBatchEmailAttachment
  }

  fun getCrimeBatch(id: UUID): CrimeBatchDto {
    val crimeBatch = crimeBatchRepository
      .findById(id)
      .orElseThrow {
        EntityNotFoundException("No crime batch found with id: $id")
      }

    val crimeBatchCrimeVersions = crimeBatchCrimeVersionRepository.findCrimeBatchCrimeVersionsByBatchId(crimeBatch.batchId).map { it -> it.crimeVersionId  }

    val crimeVersions = crimeVersionRepository.findAllById(crimeBatchCrimeVersions)

    return CrimeBatchDto(crimeBatch, crimeVersions)
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
