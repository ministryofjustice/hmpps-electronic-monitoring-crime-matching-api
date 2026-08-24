package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.deviceActivation

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.ColumnExtensions.cast
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.JoinType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SqlType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions.CurrentDate
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.DeviceActivation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.Person

class GetDeviceActivationByIdQueryBuilder(private val id: Long) {
  fun build(): AthenaQuery = DeviceActivation
    .join(Person, JoinType.INNER) {
      DeviceActivation.personId eq Person.mdssPersonId
    }
    .select(
      DeviceActivation.deviceActivationId,
      DeviceActivation.deviceId,
      DeviceActivation.deviceSerialNumber,
      Person.deviceWearerId,
      DeviceActivation.deviceActivationDate,
      DeviceActivation.deviceDeactivationDate,
    )
    .where {
      DeviceActivation.deviceActivationId eq id

      Person.groupedDate.cast(SqlType.Date) eq CurrentDate()
    }
    .prepare()
}
