package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import software.amazon.awssdk.services.athena.model.ResultSet
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.AthenaResultSetExtractor
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.formatter
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.nullableLocalDateTime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.DeviceActivation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Person
import java.time.LocalDateTime

class PersonResultSetExtractor : AthenaResultSetExtractor<Person> {
  override fun extractData(resultSet: ResultSet): List<Person> {
    val persons = mutableMapOf<String, Person>()
    val rows = getRowValues(resultSet)

    for (row in rows) {
      val person = persons.getOrPut(row[0]) {
        Person(
          personId = row[0],
          firstName = row[1],
          lastName = row[2],
          nomisId = row[3],
          pncRef = row[4],
          dateOfBirth = row[5],
          probationPractitioner = row[6],
          postcode = row[7],
          cityOrTown = row[8],
          street = row[9],
          deviceActivations = mutableListOf(),
        )
      }

      if (row.size == 14) {
        val deviceActivation = DeviceActivation(
          deviceActivationId = row[11].toLong(),
          deviceId = row[10].toLong(),
          deviceName = "",
          uniqueDeviceWearerId = row[0],
          deviceActivationDate = LocalDateTime.parse(row[12], formatter),
          deviceDeactivationDate = nullableLocalDateTime(row[13]),
          orderStart = "",
          orderEnd = "",
        )

        person.deviceActivations.add(deviceActivation)
      }
    }

    return persons.values.toList()
  }

  private fun getRowValues(resultSet: ResultSet): List<List<String>> = resultSet
    .rows()
    .drop(1)
    .map { row ->
      row
        .data()
        .map { col ->
          col.varCharValue() ?: ""
        }
    }
}
