package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.JoinType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.DeviceActivation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.Person

class GetPersonsQueryBuilder(private val personsQueryCriteria: PersonsQueryCriteria) {
  fun build(): AthenaQuery = Person
    .join(DeviceActivation, JoinType.INNER) {
      Person.mdssPersonId eq DeviceActivation.personId
    }
    .select(
      Person.deviceWearerId,
      Person.firstName,
      Person.lastName,
      Person.nomisId,
      Person.pncId,
      Person.dateOfBirth,
      Person.responsibleOfficerName,
      Person.postcode,
      Person.cityOrTown,
      Person.street,
      DeviceActivation.deviceId,
      DeviceActivation.personId,
      DeviceActivation.deviceActivationId,
      DeviceActivation.deviceActivationDate,
      DeviceActivation.deviceDeactivationDate,
    )
    .where {
      personsQueryCriteria.name?.let {
        or {
          Person.firstName like "%$it%"
          Person.lastName like "%$it%"
        }
      }

      personsQueryCriteria.nomisId?.let {
        Person.nomisId like "%$it%"
      }

      personsQueryCriteria.deviceId?.let {
        DeviceActivation.deviceId eq it
      }
    }
    .prepare()
}
