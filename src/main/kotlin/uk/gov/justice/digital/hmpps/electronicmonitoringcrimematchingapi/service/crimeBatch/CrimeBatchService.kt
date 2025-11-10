package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeRecordDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository

@Service
class CrimeBatchService(
  private val crimeBatchRepository: CrimeBatchRepository,
) {

  fun createCrimeBatch(records: List<CrimeRecordDto>) {
    val crimeBatch = CrimeBatch(
      policeForce = records[0].policeForce,
    )

    for (record in records) {
      crimeBatch.crimes.add(createCrime(record, crimeBatch))
    }

    crimeBatchRepository.save(crimeBatch)
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
}
