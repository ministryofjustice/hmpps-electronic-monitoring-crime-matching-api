package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.position

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.config.datastore.DatastoreProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.GeolocationMechanism
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@ActiveProfiles("test")
class GetPositionsByDeviceActivationIdTest {
  private val datastoreProperties = DatastoreProperties(
    fmsDatabase = "",
    mdssDatabase = "allied_mdss_dev",
    outputBucketArn = "",
    retryIntervalMs = 0,
    workgroup = ""
  )

  @Test
  fun `it should build a valid query without additional filters`() {
    val id: Long = 1
    val query = GetPositionsByDeviceActivationId(
      datastoreProperties,
      id,
      null,
      null,
      null
    ).build()

    assertThat(query.queryString).isEqualTo(
      """
      SELECT 
        p.position_id,
        p.position_latitude,
        p.position_longitude,
        p.position_precision,
        p.position_speed,
        p.position_direction,
        p.position_gps_date,
        p.position_lbs
      FROM 
        allied_mdss_dev.device_activation d
      INNER JOIN
        allied_mdss_dev.position p ON ( d.device_id = p.device_id AND d.person_id = p.person_id )
      WHERE 
        d.device_activation_id = ?
      """.trimIndent().replace("\\s+".toRegex(), " "),
    )
    assertThat(query.parameters).isEqualTo(listOf("1"))
  }

  @Test
  fun `it should build a valid query with a geoLocationMechanism filter`() {
    val id: Long = 1
    val query = GetPositionsByDeviceActivationId(
      datastoreProperties,
      id,
      GeolocationMechanism.RF,
      null,
      null
    ).build()

    assertThat(query.queryString).isEqualTo(
      """
      SELECT 
        p.position_id,
        p.position_latitude,
        p.position_longitude,
        p.position_precision,
        p.position_speed,
        p.position_direction,
        p.position_gps_date,
        p.position_lbs
      FROM 
        allied_mdss_dev.device_activation d
      INNER JOIN
        allied_mdss_dev.position p ON ( d.device_id = p.device_id AND d.person_id = p.person_id )
      WHERE 
        d.device_activation_id = ?
      AND
        p.position_lbs = ?
      """.trimIndent().replace("\\s+".toRegex(), " "),
    )
    assertThat(query.parameters).isEqualTo(listOf("1", "4"))
  }

  @Test
  fun `it should build a valid query with a from filter`() {
    val id: Long = 1
    val query = GetPositionsByDeviceActivationId(
      datastoreProperties,
      id,
      null,
      ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 1, 0, 0), ZoneOffset.UTC),
      null
    ).build()

    assertThat(query.queryString).isEqualTo(
      """
      SELECT 
        p.position_id,
        p.position_latitude,
        p.position_longitude,
        p.position_precision,
        p.position_speed,
        p.position_direction,
        p.position_gps_date,
        p.position_lbs
      FROM 
        allied_mdss_dev.device_activation d
      INNER JOIN
        allied_mdss_dev.position p ON ( d.device_id = p.device_id AND d.person_id = p.person_id )
      WHERE 
        d.device_activation_id = ?
      AND
        p.position_gps_date >= from_iso8601_timestamp(?)
      """.trimIndent().replace("\\s+".toRegex(), " "),
    )
    assertThat(query.parameters).isEqualTo(listOf("1", "'2020-01-01T01:00Z'"))
  }

  @Test
  fun `it should build a valid query with a to filter`() {
    val id: Long = 1
    val query = GetPositionsByDeviceActivationId(
      datastoreProperties,
      id,
      null,
      null,
      ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 1, 0, 0), ZoneOffset.UTC),
    ).build()

    assertThat(query.queryString).isEqualTo(
      """
      SELECT 
        p.position_id,
        p.position_latitude,
        p.position_longitude,
        p.position_precision,
        p.position_speed,
        p.position_direction,
        p.position_gps_date,
        p.position_lbs
      FROM 
        allied_mdss_dev.device_activation d
      INNER JOIN
        allied_mdss_dev.position p ON ( d.device_id = p.device_id AND d.person_id = p.person_id )
      WHERE 
        d.device_activation_id = ?
      AND
        p.position_gps_date <= from_iso8601_timestamp(?)
      """.trimIndent().replace("\\s+".toRegex(), " "),
    )
    assertThat(query.parameters).isEqualTo(listOf("1", "'2020-01-01T01:00Z'"))
  }

  @Test
  fun `it should build a valid query with all filters`() {
    val id: Long = 1
    val query = GetPositionsByDeviceActivationId(
      datastoreProperties,
      id,
      GeolocationMechanism.LBS,
      ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 1, 0, 0), ZoneOffset.UTC),
      ZonedDateTime.of(LocalDateTime.of(2020, 1, 2, 1, 0, 0), ZoneOffset.UTC),
    ).build()

    assertThat(query.queryString).isEqualTo(
      """
      SELECT 
        p.position_id,
        p.position_latitude,
        p.position_longitude,
        p.position_precision,
        p.position_speed,
        p.position_direction,
        p.position_gps_date,
        p.position_lbs
      FROM 
        allied_mdss_dev.device_activation d
      INNER JOIN
        allied_mdss_dev.position p ON ( d.device_id = p.device_id AND d.person_id = p.person_id )
      WHERE 
        d.device_activation_id = ?
      AND 
        p.position_lbs = ?
      AND
        p.position_gps_date >= from_iso8601_timestamp(?)
      AND
        p.position_gps_date <= from_iso8601_timestamp(?)
      """.trimIndent().replace("\\s+".toRegex(), " "),
    )
    assertThat(query.parameters).isEqualTo(listOf("1", "5", "'2020-01-01T01:00Z'", "'2020-01-02T01:00Z'"))
  }
}
