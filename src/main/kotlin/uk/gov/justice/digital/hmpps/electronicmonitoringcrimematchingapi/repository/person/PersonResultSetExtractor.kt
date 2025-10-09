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
          personName = row[1],
          nomisId = row[2],
          dob = row[3],
          zip = row[4],
          city = row[5],
          street = row[6],
          pncRef = "",
          probationPractitioner = "",
          deviceActivations = mutableListOf(),
        )
      }

      if (row.size == 11) {
        val deviceActivation = DeviceActivation(
          deviceActivationId = row[8].toLong(),
          deviceId = row[7].toLong(),
          deviceName = "",
          personId = row[0].toLong(),
          deviceActivationDate = LocalDateTime.parse(row[9], formatter),
          deviceDeactivationDate = nullableLocalDateTime(row[10]),
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
