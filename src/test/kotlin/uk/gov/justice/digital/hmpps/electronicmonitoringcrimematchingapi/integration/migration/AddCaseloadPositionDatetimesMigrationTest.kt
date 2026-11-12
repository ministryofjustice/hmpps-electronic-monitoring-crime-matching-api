package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import java.time.OffsetDateTime
import java.util.UUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddCaseloadPositionDatetimesMigrationTest {

  private val postgres = PostgreSQLContainer<Nothing>("postgres:17")

  private lateinit var dataSource: DataSource
  private lateinit var jdbcTemplate: JdbcTemplate

  private val deviceWearerId = UUID.randomUUID()
  private val existingPositionId = UUID.randomUUID()

  @BeforeAll
  fun setup() {
    postgres.start()

    dataSource = DriverManagerDataSource().apply {
      setDriverClassName("org.postgresql.Driver")
      url = postgres.jdbcUrl
      username = postgres.username
      password = postgres.password
    }
    jdbcTemplate = JdbcTemplate(dataSource)

    Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration")
      .target("19")
      .load()
      .migrate()

    val ingestionAttemptId = UUID.randomUUID()
    val emailId = UUID.randomUUID()
    val attachmentId = UUID.randomUUID()
    val batchId = UUID.randomUUID()
    val crimeId = UUID.randomUUID()
    val crimeVersionId = UUID.randomUUID()
    val runId = UUID.randomUUID()
    val resultId = UUID.randomUUID()

    jdbcTemplate.update("INSERT INTO crime_batch_ingestion_attempt (id) VALUES (?)", ingestionAttemptId)
    jdbcTemplate.update("INSERT INTO crime_batch_email (id, crime_batch_ingestion_attempt_id) VALUES (?, ?)", emailId, ingestionAttemptId)
    jdbcTemplate.update("INSERT INTO crime_batch_email_attachment (id, crime_batch_email_id, row_count) VALUES (?, ?, 1)", attachmentId, emailId)
    jdbcTemplate.update("INSERT INTO crime_batch (id, batch_id, crime_batch_email_attachment_id) VALUES (?, 'batch', ?)", batchId, attachmentId)
    jdbcTemplate.update("INSERT INTO crime (id, police_force_area, crime_reference) VALUES (?, 'area', 'reference')", crimeId)
    jdbcTemplate.update("INSERT INTO crime_version (id, crime_id, crime_batch_id, crime_type_id) VALUES (?, ?, ?, 'type')", crimeVersionId, crimeId, batchId)
    jdbcTemplate.update(
      """
      INSERT INTO crime_matching_run
        (id, crime_batch_id, algorithm_version, trigger_type, status, created_at, matching_started, matching_ended)
      VALUES (?, ?, '1', 'test', 'complete', NOW(), NOW(), NOW())
      """.trimIndent(),
      runId,
      batchId,
    )
    jdbcTemplate.update(
      "INSERT INTO crime_matching_result (id, crime_version_id, crime_matching_run_id, created_at) VALUES (?, ?, ?, NOW())",
      resultId,
      crimeVersionId,
      runId,
    )
    jdbcTemplate.update(
      """
      INSERT INTO crime_matching_result_device_wearer
        (id, crime_matching_result_id, address, device_id, device_serial_number, device_name, identifier, name, nomis_id, pnc_ref)
      VALUES (?, ?, 'address', 1, 'serial', 'device', 'identifier', 'name', 'nomis', 'pnc')
      """.trimIndent(),
      deviceWearerId,
      resultId,
    )
    jdbcTemplate.update(
      """
      INSERT INTO crime_matching_result_position
        (id, crime_matching_result_device_wearer_id, captured_date_time, direction, latitude, longitude, precision, sequence_label, speed)
      VALUES (?, ?, NOW(), 0, 0, 0, 0, 'existing', 0)
      """.trimIndent(),
      existingPositionId,
      deviceWearerId,
    )

    Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration")
      .target("20")
      .load()
      .migrate()
  }

  @AfterAll
  fun tearDown() {
    postgres.stop()
  }

  @Test
  fun `it should leave the new timestamps null for existing positions`() {
    val timestamps = positionTimestamps(existingPositionId)

    assertThat(timestamps.first).isNull()
    assertThat(timestamps.second).isNull()
  }

  @Test
  fun `it should store the new timestamps for new positions`() {
    val positionId = UUID.randomUUID()
    val caseloadDatetime = OffsetDateTime.parse("2025-01-02T03:04:05+01:00")
    val positionDatetime = OffsetDateTime.parse("2025-02-03T04:05:06-05:00")

    jdbcTemplate.update(
      """
      INSERT INTO crime_matching_result_position
        (id, crime_matching_result_device_wearer_id, captured_date_time, direction, latitude, longitude, precision, sequence_label, speed,
         caseload_datetime_added, position_datetime_added)
      VALUES (?, ?, NOW(), 0, 0, 0, 0, 'new', 0, ?, ?)
      """.trimIndent(),
      positionId,
      deviceWearerId,
      caseloadDatetime,
      positionDatetime,
    )

    val timestamps = positionTimestamps(positionId)
    assertThat(timestamps.first?.toInstant()).isEqualTo(caseloadDatetime.toInstant())
    assertThat(timestamps.second?.toInstant()).isEqualTo(positionDatetime.toInstant())
  }

  private fun positionTimestamps(positionId: UUID): Pair<OffsetDateTime?, OffsetDateTime?> = jdbcTemplate.queryForObject(
    "SELECT caseload_datetime_added, position_datetime_added FROM crime_matching_result_position WHERE id = ?",
    { rs, _ ->
      rs.getObject("caseload_datetime_added", OffsetDateTime::class.java) to
        rs.getObject("position_datetime_added", OffsetDateTime::class.java)
    },
    positionId,
  )
}
