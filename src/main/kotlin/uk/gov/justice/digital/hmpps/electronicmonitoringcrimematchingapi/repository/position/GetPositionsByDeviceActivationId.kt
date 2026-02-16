package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.position

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.datastore.DatastoreProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.JoinType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.SqlQueryBuilder
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.GeolocationMechanism
import java.time.ZonedDateTime

class GetPositionsByDeviceActivationId : SqlQueryBuilder {
  constructor(
    datastoreProperties: DatastoreProperties,
    id: Long,
    geolocationMechanism: GeolocationMechanism?,
    from: ZonedDateTime?,
    to: ZonedDateTime?,
  ) : super("${datastoreProperties.mdssDatabase}.device_activation", "d") {
    this.addFields(
      listOf(
        "p.position_id",
        "p.position_latitude",
        "p.position_longitude",
        "p.position_precision",
        "p.position_speed",
        "p.position_direction",
        "p.position_gps_date",
        "p.position_lbs",
      ),
    )
      .addJoin(
        "${datastoreProperties.mdssDatabase}.position p",
        "d.device_id = p.device_id AND d.person_id = p.person_id",
        JoinType.INNER,
      )
      .addFilter("d.device_activation_id", id)

    if (geolocationMechanism != null) {
      this.addFilter("p.position_lbs", geolocationMechanism.value)
    }

    if (from != null) {
      this.greaterEq("p.position_gps_date", from)
    }

    if (to != null) {
      this.lessEq("p.position_gps_date", to)
    }
  }
}
