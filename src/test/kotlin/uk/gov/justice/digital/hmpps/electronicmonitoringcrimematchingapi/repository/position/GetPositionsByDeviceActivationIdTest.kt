package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.position

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.GeolocationMechanism
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

@ActiveProfiles("test")
class GetPositionsByDeviceActivationIdTest {
  @Test
  fun `it should build a valid query without additional filters`() {
    val id: Long = 1
    val query = GetPositionsByDeviceActivationId(
      id,
      null,
      null,
      null,
    ).build()

    assertThat(query.queryString).isEqualTo(
      """
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
      """.trimIndent().replace("\\s+".toRegex(), " "),
    )
    assertThat(query.parameters).isEqualTo(listOf("1"))
  }

  @Test
  fun `it should build a valid query with a geoLocationMechanism filter`() {
    val id: Long = 1
    val query = GetPositionsByDeviceActivationId(
      id,
      GeolocationMechanism.RF,
      null,
      null,
    ).build()

    assertThat(query.queryString).isEqualTo(
      """
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
      """.trimIndent().replace("\\s+".toRegex(), " "),
    )
    assertThat(query.parameters).isEqualTo(listOf("1", "4"))
  }

  @Test
  fun `it should build a valid query with a from filter`() {
    val id: Long = 1
    val query = GetPositionsByDeviceActivationId(
      id,
      null,
      ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 1, 0, 0), ZoneOffset.UTC),
      null,
    ).build()

    assertThat(query.queryString).isEqualTo(
      """
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
      """.trimIndent().replace("\\s+".toRegex(), " "),
    )
    assertThat(query.parameters).isEqualTo(listOf("1", "'2020-01-01T01:00Z'"))
  }

  @Test
  fun `it should build a valid query with a to filter`() {
    val id: Long = 1
    val query = GetPositionsByDeviceActivationId(
      id,
      null,
      null,
      ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 1, 0, 0), ZoneOffset.UTC),
    ).build()

    assertThat(query.queryString).isEqualTo(
      """
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
      """.trimIndent().replace("\\s+".toRegex(), " "),
    )
    assertThat(query.parameters).isEqualTo(listOf("1", "'2020-01-01T01:00Z'"))
  }

  @Test
  fun `it should build a valid query with all filters`() {
    val id: Long = 1
    val query = GetPositionsByDeviceActivationId(
      id,
      GeolocationMechanism.LBS,
      ZonedDateTime.of(LocalDateTime.of(2020, 1, 1, 1, 0, 0), ZoneOffset.UTC),
      ZonedDateTime.of(LocalDateTime.of(2020, 1, 2, 1, 0, 0), ZoneOffset.UTC),
    ).build()

    assertThat(query.queryString).isEqualTo(
      """
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
      """.trimIndent().replace("\\s+".toRegex(), " "),
    )
    assertThat(query.parameters).isEqualTo(listOf("1", "5", "'2020-01-01T01:00Z'", "'2020-01-02T01:00Z'"))
  }
}
