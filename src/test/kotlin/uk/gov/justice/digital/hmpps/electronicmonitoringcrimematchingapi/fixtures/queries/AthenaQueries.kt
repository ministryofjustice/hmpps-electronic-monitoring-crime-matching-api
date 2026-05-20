package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.fixtures.queries

object AthenaQueries {
  val SelectDeviceActivationById = """
    SELECT 
      device_activations.device_activation_id, 
      device_activations.device_id, 
      device_activations.person_id, 
      device_activations.device_activation_date, 
      device_activations.device_deactivation_date 
    FROM 
      device_activations
    WHERE 
      device_activations.device_activation_id = ?
  """.trimIndent().replace("\\s+".toRegex(), " ")

  val SelectPersonById = """
    SELECT
      caseload.unique_device_wearer_id,
      caseload.first_name,
      caseload.last_name, 
      caseload.nomis_id,
      caseload.pnc_id,
      caseload.date_of_birth,
      caseload.responsible_officer_name,
      caseload.postcode,
      caseload.city_or_town,
      caseload.house_number_and_street_name
    FROM 
      caseload
    WHERE 
      caseload.unique_device_wearer_id = ?
  """.trimIndent().replace("\\s+".toRegex(), " ")

  val SelectPersonsByNameLike = """
    SELECT 
      caseload.unique_device_wearer_id, 
      caseload.first_name, 
      caseload.last_name, 
      caseload.nomis_id, 
      caseload.pnc_id,
      caseload.date_of_birth,
      caseload.responsible_officer_name,
      caseload.postcode, 
      caseload.city_or_town, 
      caseload.house_number_and_street_name, 
      device_activations.device_id,
      device_activations.person_id,
      device_activations.device_activation_id, 
      device_activations.device_activation_date, 
      device_activations.device_deactivation_date 
    FROM 
      caseload 
    INNER JOIN 
      device_activations ON caseload.mdss_person_id = device_activations.person_id 
    WHERE ( caseload.first_name LIKE ? OR caseload.last_name LIKE ? )
  """.trimIndent().replace("\\s+".toRegex(), " ")

  val SelectPersonsByNomisIdLike = """
    SELECT 
      caseload.unique_device_wearer_id, 
      caseload.first_name, 
      caseload.last_name, 
      caseload.nomis_id, 
      caseload.pnc_id,
      caseload.date_of_birth,
      caseload.responsible_officer_name,
      caseload.postcode, 
      caseload.city_or_town, 
      caseload.house_number_and_street_name, 
      device_activations.device_id,
      device_activations.person_id,
      device_activations.device_activation_id, 
      device_activations.device_activation_date, 
      device_activations.device_deactivation_date 
    FROM 
      caseload 
    INNER JOIN 
      device_activations ON caseload.mdss_person_id = device_activations.person_id 
    WHERE caseload.nomis_id LIKE ?
  """.trimIndent().replace("\\s+".toRegex(), " ")

  val SelectPersonsByDeviceIdLike = """
    SELECT 
      caseload.unique_device_wearer_id, 
      caseload.first_name, 
      caseload.last_name, 
      caseload.nomis_id, 
      caseload.pnc_id,
      caseload.date_of_birth,
      caseload.responsible_officer_name,
      caseload.postcode, 
      caseload.city_or_town, 
      caseload.house_number_and_street_name, 
      device_activations.device_id,
      device_activations.person_id,
      device_activations.device_activation_id, 
      device_activations.device_activation_date, 
      device_activations.device_deactivation_date 
    FROM 
      caseload 
    INNER JOIN 
      device_activations ON caseload.mdss_person_id = device_activations.person_id 
    WHERE device_activations.device_id = ?
  """.trimIndent().replace("\\s+".toRegex(), " ")

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
