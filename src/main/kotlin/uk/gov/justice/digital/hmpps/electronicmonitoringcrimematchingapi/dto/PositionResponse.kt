package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.GeolocationMechanism
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Position

data class PositionResponse(
  val positionId: Long,
  val latitude: Double,
  val longitude: Double,
  val precision: Long,
  val speed: Long,
  val direction: Long,
  val timestamp: String,
  val geolocationMechanism: String,
) {
  constructor(position: Position) : this(
    positionId = position.positionId,
    latitude = position.positionLatitude,
    longitude = position.positionLongitude,
    precision = position.positionPrecision,
    speed = position.positionSpeed,
    direction = position.positionDirection,
    timestamp = position.positionGpsDate.toString(),
    geolocationMechanism = GeolocationMechanism.from(position.positionLbs).toString(),
  )
}
