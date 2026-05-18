package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.position

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.fixtures.queries.AthenaQueries
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
      AthenaQueries.SelectPositionsByDeviceActivationId,
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
      AthenaQueries.SelectPositionsByDeviceActivationIdAndGeolocationMechanism,
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
      AthenaQueries.SelectPositionsByDeviceActivationIdAndFromDate,
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
      AthenaQueries.SelectPositionsByDeviceActivationIdAndToDate,
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
      AthenaQueries.SelectPositionsByDeviceActivationIdAndGeolocationMechanismAndFromDateAndToDate,
    )
    assertThat(query.parameters).isEqualTo(listOf("1", "5", "'2020-01-01T01:00Z'", "'2020-01-02T01:00Z'"))
  }
}
