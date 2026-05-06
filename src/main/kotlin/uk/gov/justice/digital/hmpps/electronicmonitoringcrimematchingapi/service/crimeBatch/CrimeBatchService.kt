package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttemptSummary
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersionUpdate
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeVersionFieldName
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.projection.CrimeBatchIngestionAttemptSummaryProjection
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.MatchingNotificationService
import java.time.LocalDateTime
import java.util.UUID

@Service
class CrimeBatchService(
  private val crimeBatchRepository: CrimeBatchRepository,
  private val crimeRepository: CrimeRepository,
  private val crimeVersionRepository: CrimeVersionRepository,
  private val matchingNotificationService: MatchingNotificationService,
  private val crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository,
) {

  @Transactional
  fun createCrimeBatch(records: List<CrimeRecordRequest>, crimeBatchEmailAttachment: CrimeBatchEmailAttachment): CrimeBatch {
    // Create a new batch
    val crimeBatch = CrimeBatch(
      batchId = records.first().batchId,
      crimeBatchEmailAttachment = crimeBatchEmailAttachment,
    )

    for (record in records) {
      // Check for existing crime else save new crime
      val crime = crimeRepository.findByCrimeReferenceAndPoliceForceArea(record.crimeReference, record.policeForce)
        .orElseGet { crimeRepository.save(Crime(policeForceArea = record.policeForce, crimeReference = record.crimeReference)) }

      val previousVersion = crimeVersionRepository.findFirstByCrimeIdOrderByCreatedAtDesc(crime.id)

      val crimeVersion = createCrimeVersion(
        record,
        crime,
        crimeBatch,
      )

      if (previousVersion != null) {
        compareCrimeVersions(crimeVersion, previousVersion)
      }

      // Add version to batch
      crimeBatch.crimeVersions.add(crimeVersion)
    }

    // Save batch
    crimeBatchRepository.save(crimeBatch)

    matchingNotificationService.publishMatchingRequest(crimeBatch.id.toString())

    return crimeBatch
  }

  fun getCrimeBatch(id: String): CrimeBatch {
    val crimeBatch = crimeBatchRepository
      .findById(UUID.fromString(id))
      .orElseThrow {
        EntityNotFoundException("No crime batch found with id: $id")
      }

    return crimeBatch
  }

  fun getCrimeBatchIngestionAttemptSummaries(
    batchId: String?,
    policeForceArea: String?,
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    page: Int,
    pageSize: Int,
  ): Page<CrimeBatchIngestionAttemptSummaryProjection> = crimeBatchIngestionAttemptRepository.findCrimeBatchIngestionAttempts(
    batchId = batchId,
    policeForceArea = policeForceArea,
    fromDate = fromDate,
    toDate = toDate,
    pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")),
  )

  fun getCrimeBatchIngestionAttempt(
    id: UUID,
  ): CrimeBatchIngestionAttemptSummary {
    val crimeBatchIngestionAttempt = crimeBatchIngestionAttemptRepository
      .findCrimeBatchIngestionAttemptById(id)
      .orElseThrow {
        EntityNotFoundException("No crime batch ingestion attempt found with id: $id")
      }

    return CrimeBatchIngestionAttemptSummary(
      ingestionAttempt = crimeBatchIngestionAttempt,
      validationErrors = crimeBatchIngestionAttemptRepository.findIngestionAttemptValidationErrors(id),
      crimesByCrimeType = crimeBatchIngestionAttemptRepository.findIngestionAttemptCrimesByType(id),
    )
  }

  private fun createCrimeVersion(record: CrimeRecordRequest, crime: Crime, crimeBatch: CrimeBatch): CrimeVersion = CrimeVersion(
    crime = crime,
    crimeTypeId = record.crimeTypeId,
    crimeDateTimeFrom = record.crimeDateTimeFrom,
    crimeDateTimeTo = record.crimeDateTimeTo,
    easting = record.easting,
    northing = record.northing,
    latitude = record.latitude,
    longitude = record.longitude,
    crimeText = record.crimeText,
    crimeBatch = crimeBatch,
  )

  private fun compareCrimeVersions(
    version: CrimeVersion,
    previousVersion: CrimeVersion,
  ) {
    val comparisonFields = listOf(
      CrimeVersionFieldName.CRIME_TYPE_ID to CrimeVersion::crimeTypeId,
      CrimeVersionFieldName.CRIME_DATE_TIME_FROM to CrimeVersion::crimeDateTimeFrom,
      CrimeVersionFieldName.CRIME_DATE_TIME_TO to CrimeVersion::crimeDateTimeTo,
      CrimeVersionFieldName.EASTING to CrimeVersion::easting,
      CrimeVersionFieldName.NORTHING to CrimeVersion::northing,
      CrimeVersionFieldName.LATITUDE to CrimeVersion::latitude,
      CrimeVersionFieldName.LONGITUDE to CrimeVersion::longitude,
      CrimeVersionFieldName.CRIME_TEXT to CrimeVersion::crimeText,
    )

    comparisonFields.forEach { (fieldName, prop) ->
      if (prop.get(previousVersion) != prop.get(version)) {
        version.updates.add(createCrimeVersionUpdate(fieldName, version))
      }
    }
  }

  private fun createCrimeVersionUpdate(crimeVersionFieldName: CrimeVersionFieldName, crimeVersion: CrimeVersion): CrimeVersionUpdate = CrimeVersionUpdate(fieldName = crimeVersionFieldName, crimeVersion = crimeVersion)
}
