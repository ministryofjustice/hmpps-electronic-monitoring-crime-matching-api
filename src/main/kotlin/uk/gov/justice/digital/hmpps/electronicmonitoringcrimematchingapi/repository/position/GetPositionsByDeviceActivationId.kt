package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.position

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.JoinType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.functions.AthenaFunctions
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.AthenaQuery
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.DeviceActivation
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena.Position
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.GeolocationMechanism
import java.time.ZonedDateTime

class GetPositionsByDeviceActivationId(
  private val id: Long,
  private val geolocationMechanism: GeolocationMechanism?,
  private val from: ZonedDateTime?,
  private val to: ZonedDateTime?,
) {
  fun build(): AthenaQuery = DeviceActivation
    .join(Position, JoinType.INNER) {
      DeviceActivation.deviceId eq Position.deviceId
      DeviceActivation.personId eq Position.personId
    }
    .select(
      Position.positionId,
      Position.positionLatitude,
      Position.positionLongitude,
      Position.positionPrecision,
      Position.positionSpeed,
      Position.positionDirection,
      Position.positionRecordedDate,
      Position.positionLbs,
    )
    .where {
      DeviceActivation.deviceActivationId eq id

      geolocationMechanism?.let {
        Position.positionLbs eq geolocationMechanism.value
      }

      from?.let {
        Position.positionRecordedDate gte AthenaFunctions.fromIso8601Timestamp(from)
      }

      to?.let {
        Position.positionRecordedDate lte AthenaFunctions.fromIso8601Timestamp(to)
      }
    }
    .prepare()
}
