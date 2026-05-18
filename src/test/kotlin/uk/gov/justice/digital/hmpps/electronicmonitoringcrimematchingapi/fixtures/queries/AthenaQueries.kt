package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.fixtures.queries

object AthenaQueries {
  val SelectPositionsByDeviceActivationId = """
    SELECT 
      position.position_id,
      position.position_latitude,
      position.position_longitude,
      position.position_precision,
      position.position_speed,
      position.position_direction,
      position.position_recorded_date,
      position.position_lbs
    FROM 
      device_activations
    INNER JOIN
      position ON ( device_activations.device_id = position.device_id AND device_activations.person_id = position.person_id )
    WHERE 
      device_activations.device_activation_id = ?
  """.trimIndent().replace("\\s+".toRegex(), " ")

  val SelectPositionsByDeviceActivationIdAndGeolocationMechanism = """
    SELECT 
      position.position_id,
      position.position_latitude,
      position.position_longitude,
      position.position_precision,
      position.position_speed,
      position.position_direction,
      position.position_recorded_date,
      position.position_lbs
    FROM 
      device_activations
    INNER JOIN
      position ON ( device_activations.device_id = position.device_id AND device_activations.person_id = position.person_id )
    WHERE (
      device_activations.device_activation_id = ?
      AND position.position_lbs = ? 
    )
  """.trimIndent().replace("\\s+".toRegex(), " ")

  val SelectPositionsByDeviceActivationIdAndFromDate = """
    SELECT 
      position.position_id,
      position.position_latitude,
      position.position_longitude,
      position.position_precision,
      position.position_speed,
      position.position_direction,
      position.position_recorded_date,
      position.position_lbs
    FROM 
      device_activations
    INNER JOIN
      position ON ( device_activations.device_id = position.device_id AND device_activations.person_id = position.person_id )
    WHERE (
      device_activations.device_activation_id = ?
      AND position.position_recorded_date >= from_iso8601_timestamp(?) 
    )
  """.trimIndent().replace("\\s+".toRegex(), " ")

  val SelectPositionsByDeviceActivationIdAndToDate = """
    SELECT 
      position.position_id,
      position.position_latitude,
      position.position_longitude,
      position.position_precision,
      position.position_speed,
      position.position_direction,
      position.position_recorded_date,
      position.position_lbs
    FROM 
      device_activations
    INNER JOIN
      position ON ( device_activations.device_id = position.device_id AND device_activations.person_id = position.person_id )
    WHERE (
      device_activations.device_activation_id = ?
      AND position.position_recorded_date <= from_iso8601_timestamp(?) 
    )
  """.trimIndent().replace("\\s+".toRegex(), " ")

  val SelectPositionsByDeviceActivationIdAndGeolocationMechanismAndFromDateAndToDate = """
    SELECT 
      position.position_id,
      position.position_latitude,
      position.position_longitude,
      position.position_precision,
      position.position_speed,
      position.position_direction,
      position.position_recorded_date,
      position.position_lbs
    FROM 
      device_activations
    INNER JOIN
      position ON ( device_activations.device_id = position.device_id AND device_activations.person_id = position.person_id )
    WHERE (
      device_activations.device_activation_id = ?
      AND position.position_lbs = ?
      AND position.position_recorded_date >= from_iso8601_timestamp(?)
      AND position.position_recorded_date <= from_iso8601_timestamp(?)
    )
  """.trimIndent().replace("\\s+".toRegex(), " ")
}
