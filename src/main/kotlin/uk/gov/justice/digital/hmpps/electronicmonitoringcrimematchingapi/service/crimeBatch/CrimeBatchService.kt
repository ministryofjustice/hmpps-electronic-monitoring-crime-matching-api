package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeBatch

import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import java.io.InputStream

@Service
class CrimeBatchService(
  private val repository: CrimeBatchRepository,
  @Value("\${crime-data.batch.size}") val batchSize: Int,
) {

  @Transactional
  fun ingestCsvData(csvData: InputStream) {
    val validBatch = mutableListOf<CrimeBatch>()

    CSVParser.parse(csvData, Charsets.UTF_8, CSVFormat.DEFAULT)
      .asSequence()
      .forEach { record ->
        try {
          val crimeBatch = CrimeBatch(
            policeForce = record[0],
            crimeTypeId = record[1],
            crimeTypeDescription = record[2],
            batchId = record[3],
            crimeId = record[4],
            crimeDateTimeFrom = record[5],
            crimeDateTimeTo = record[6],
            easting = record[7],
            northing = record[8],
            latitude = record[9],
            longitude = record[10],
            datum = record[11],
            crimeText = record[12],
          )

          validBatch.add(crimeBatch)

          if (validBatch.size >= batchSize) {
            repository.saveAll(validBatch)
            validBatch.clear()
          }
        } catch (e: Exception) {
          throw ValidationException("Error parsing csv data: ${e.message}")
        }
      }

    if (validBatch.isNotEmpty()) {
      repository.saveAll(validBatch)
    }
  }

}