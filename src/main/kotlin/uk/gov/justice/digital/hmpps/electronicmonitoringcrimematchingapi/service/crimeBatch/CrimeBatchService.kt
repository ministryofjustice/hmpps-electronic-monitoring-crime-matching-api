package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import jakarta.transaction.Transactional
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import java.io.InputStream
import java.util.UUID

@Service
class CrimeBatchService(
  private val crimeBatchRepository: CrimeBatchRepository,
) {

  @Transactional
  fun ingestCsvData(csvData: InputStream) {
    val parser = CSVParser.parse(csvData, Charsets.UTF_8, CSVFormat.DEFAULT)
    val records = parser.records

    val firstRecord = records.first()
    val crimeBatch = CrimeBatch(
      id = UUID.randomUUID().toString(),
      policeForce = firstRecord[0],
    )

    records.forEach { record ->
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

        crimeBatch.crimes.add(crime)
      } catch (e: Exception) {
        // TODO handle invalid rows
      }
    }

    crimeBatchRepository.save(crimeBatch)
  }
}
