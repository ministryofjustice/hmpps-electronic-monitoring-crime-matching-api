package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.MatchingNotificationService
import java.util.UUID

@Service
class CrimeBatchService(
  private val crimeBatchRepository: CrimeBatchRepository,
  private val matchingNotificationService: MatchingNotificationService,
) {

  fun createCrimeBatch(records: List<CrimeRecordDto>) {
    val crimeBatch = CrimeBatch(
      id = UUID.randomUUID().toString(),
      policeForce = records[0].policeForce,
    )

    for (record in records) {
      crimeBatch.crimes.add(createCrime(record, crimeBatch))
    }

    crimeBatchRepository.save(crimeBatch)

    matchingNotificationService.publishMatchingRequest(crimeBatch.id)
  }

  fun createCrime(record: CrimeRecordDto, batch: CrimeBatch): Crime = Crime(
    crimeTypeId = record.crimeTypeId,
    crimeReference = record.crimeReference,
    crimeDateTimeFrom = record.crimeDateTimeFrom,
    crimeDateTimeTo = record.crimeDateTimeTo,
    easting = record.easting,
    northing = record.northing,
    latitude = record.latitude,
    longitude = record.longitude,
    datum = record.datum,
    crimeText = record.crimeText,
    crimeBatch = batch,
  )

  fun getCrimeBatch(id: String): CrimeBatch = this.crimeBatchRepository
    .findById(id)
    .orElseThrow {
      EntityNotFoundException("No crime batch found with id: $id")
    }
}
