package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.person

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.JoinType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.Caseload
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.DeviceActivation

class GetPersonsQueryBuilder(private val personsQueryCriteria: PersonsQueryCriteria) {
  fun build(): AthenaQuery {
    val columns = mutableListOf(
      Caseload.personId,
      Caseload.firstName,
      Caseload.lastName,
      Caseload.nomisId,
      Caseload.dateOfBirth,
      Caseload.postcode,
      Caseload.cityOrTown,
      Caseload.street,
    )

    if (personsQueryCriteria.includeDeviceActivations) {
      columns.add(DeviceActivation.deviceId)
      columns.add(DeviceActivation.deviceActivationId)
      columns.add(DeviceActivation.deviceActivationDate)
      columns.add(DeviceActivation.deviceDeactivationDate)
    }

    val caseload = if (!personsQueryCriteria.deviceId.isNullOrBlank()) {
      Caseload.join(DeviceActivation, JoinType.INNER) {
        Caseload.personId eq DeviceActivation.personId
      }
    } else {
      Caseload
    }

    return caseload
      .select(*columns.toTypedArray())
      .where {
        if (!personsQueryCriteria.name.isNullOrBlank()) {
          or {
            Caseload.firstName like personsQueryCriteria.name
            Caseload.lastName like personsQueryCriteria.name
          }
        }

        if (!personsQueryCriteria.nomisId.isNullOrBlank()) {
          Caseload.nomisId like personsQueryCriteria.nomisId
        }

        if (!personsQueryCriteria.deviceId.isNullOrBlank()) {
          DeviceActivation.deviceId.asVarchar() like personsQueryCriteria.deviceId
        }
      }
      .prepare()
  }
}
