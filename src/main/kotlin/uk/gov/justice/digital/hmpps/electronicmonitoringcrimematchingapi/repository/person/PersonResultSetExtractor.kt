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
          personId = row[0].toLong(),
          firstName = row[1],
          lastName = row[2],
          nomisId = row[3],
          dateOfBirth = row[4],
          postcode = row[5],
          cityOrTown = row[6],
          street = row[7],
          pncRef = "",
          probationPractitioner = "",
          deviceActivations = mutableListOf(),
        )
      }

      if (row.size == 12) {
        val deviceActivation = DeviceActivation(
          deviceActivationId = row[9].toLong(),
          deviceId = row[8].toLong(),
          deviceName = "",
          personId = row[0].toLong(),
          deviceActivationDate = LocalDateTime.parse(row[10], formatter),
          deviceDeactivationDate = nullableLocalDateTime(row[11]),
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
