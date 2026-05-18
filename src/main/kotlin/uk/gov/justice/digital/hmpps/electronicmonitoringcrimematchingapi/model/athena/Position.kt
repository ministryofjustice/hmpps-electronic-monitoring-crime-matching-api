package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Table

object Position : Table("position") {
  val deviceId = long("device_id")
  val personId = long("person_id")
  val positionId = long("position_id")
  val positionLatitude = double("position_latitude")
  val positionLongitude = double("position_longitude")
  val positionPrecision = long("position_precision")
  val positionSpeed = long("position_speed")
  val positionDirection = long("position_direction")
  val positionRecordedDate = date("position_recorded_date")
  val positionLbs = long("position_lbs")
}
