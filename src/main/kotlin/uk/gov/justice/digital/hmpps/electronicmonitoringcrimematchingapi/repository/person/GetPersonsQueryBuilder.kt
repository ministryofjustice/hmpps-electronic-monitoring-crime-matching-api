package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.ExpressionExtensions.lower
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
      DeviceActivation.deviceActivationId,
      DeviceActivation.deviceSerialNumber,
      DeviceActivation.deviceActivationDate,
      DeviceActivation.deviceDeactivationDate,
    )
    .where {
      personsQueryCriteria.name
        ?.trim()
        ?.split(Regex("\\s+"))
        ?.filter { it.isNotBlank() }
        ?.forEach { token ->
          or {
            Person.firstName.lower() like "%${token.lowercase()}%"
            Person.lastName.lower() like "%${token.lowercase()}%"
          }
        }

      personsQueryCriteria.nomisId?.let {
        Person.nomisId.lower() like "%${it.lowercase()}%"
      }

      personsQueryCriteria.deviceId?.let {
        DeviceActivation.deviceSerialNumber eq it
      }
    }
    .orderBy {
      DeviceActivation.deviceActivationDate.desc
    }
    .prepare()
}
