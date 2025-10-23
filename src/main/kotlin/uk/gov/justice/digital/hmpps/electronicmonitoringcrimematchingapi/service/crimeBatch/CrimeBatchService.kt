package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import java.io.InputStream
import java.util.UUID

private const val BATCH_SIZE = 100

@Service
class CrimeBatchService(
  private val crimeBatchRepository: CrimeBatchRepository,
  private val crimeRepository: CrimeRepository,
) {

  @Transactional
  fun ingestCsvData(csvData: InputStream) {
    val parser = CSVParser.parse(csvData, Charsets.UTF_8, CSVFormat.DEFAULT)
    val records = parser.records

    if (records.isEmpty()) throw ValidationException("No records found in csv data")

    val firstRecord = records.first()
    val crimeBatch = CrimeBatch(
      id = UUID.randomUUID().toString(),
      policeForce = firstRecord[0],
    )

    crimeBatchRepository.save(crimeBatch)

    val validBatch = mutableListOf<Crime>()

    records
      .forEach { record ->
        try {
          val crime = Crime(
            crimeTypeId = record[1],
            crimeTypeDescription = record[2],
            crimeReference = record[4],
            crimeDateTimeFrom = record[5],
            crimeDateTimeTo = record[6],
            easting = record[7],
            northing = record[8],
            latitude = record[9],
            longitude = record[10],
            datum = record[11],
            crimeText = record[12],
            crimeBatch = crimeBatch,
          )
          validBatch.add(crime)

          if (validBatch.size >= BATCH_SIZE) {
            crimeRepository.saveAll(validBatch)
            validBatch.clear()
          }
        } catch (e: Exception) {
          // TODO handle invalid rows
        }
      }

    if (validBatch.isNotEmpty()) {
      crimeRepository.saveAll(validBatch)
    }
  }
}
