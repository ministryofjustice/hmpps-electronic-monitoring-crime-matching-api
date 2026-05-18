package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.JoinType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.DeviceActivation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.Person

class GetPersonsQueryBuilder(private val personsQueryCriteria: PersonsQueryCriteria) {
  fun build(): AthenaQuery {
    val columns = buildList {
      addAll(
        listOf(
          Person.personId,
          Person.firstName,
          Person.lastName,
          Person.nomisId,
          Person.dateOfBirth,
          Person.postcode,
          Person.cityOrTown,
          Person.street,
        ),
      )

      if (personsQueryCriteria.includeDeviceActivations) {
        addAll(
          listOf(
            DeviceActivation.deviceId,
            DeviceActivation.deviceActivationId,
            DeviceActivation.deviceActivationDate,
            DeviceActivation.deviceDeactivationDate,
          ),
        )
      }
    }

    return (
      if (personsQueryCriteria.includeDeviceActivations) {
        Person.join(DeviceActivation, JoinType.INNER) {
          Person.personId eq DeviceActivation.personId
        }
      } else {
        Person
      }
      )
      .select(*columns.toTypedArray())
      .where {
        personsQueryCriteria.name?.let {
          or {
            Person.firstName like it
            Person.lastName like it
          }
        }

        personsQueryCriteria.nomisId?.let {
          Person.nomisId like it
        }

        personsQueryCriteria.deviceId
          ?.takeIf { personsQueryCriteria.includeDeviceActivations }
          ?.let {
            DeviceActivation.deviceId.castAs("VARCHAR") like it
          }
      }
      .prepare()
  }
}
