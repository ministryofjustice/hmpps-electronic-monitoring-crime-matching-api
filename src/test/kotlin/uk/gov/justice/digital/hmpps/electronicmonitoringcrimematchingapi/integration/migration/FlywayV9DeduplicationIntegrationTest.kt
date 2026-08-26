package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.migration

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID

class FlywayV9DeduplicationIntegrationTest {

  companion object {
    private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:18")
      .apply {
        withUsername("postgres")
        withPassword("postgres")
        withDatabaseName("testdb")
      }

    @JvmStatic
    @BeforeAll
    fun beforeAll() {
      postgresContainer.start()
    }

    @JvmStatic
    @AfterAll
    fun afterAll() {
      postgresContainer.stop()
    }
  }

  private lateinit var jdbcTemplate: JdbcTemplate

  @BeforeEach
  fun setup() {
    val dataSource = DriverManagerDataSource(
      postgresContainer.jdbcUrl,
      postgresContainer.username,
      postgresContainer.password,
    )
    jdbcTemplate = JdbcTemplate(dataSource)

    flyway(targetVersion = "8").clean()
    flyway(targetVersion = "8").migrate()
  }

  @Test
  fun `it keeps historical duplicates when migrating only to v9`() {
    val seeded = seedDuplicateAttempts()

    flyway(targetVersion = "9").migrate()

    val attemptsForSource = jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM crime_batch_ingestion_attempt WHERE bucket = ? AND object_name = ?",
      Int::class.java,
      seeded.sharedBucket,
      seeded.sharedObjectName,
    )
    assertThat(attemptsForSource).isEqualTo(2)

    val hasMatchingPublishState = jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'crime_batch_ingestion_attempt' AND column_name = 'matching_publish_state'",
      Int::class.java,
    )
    assertThat(hasMatchingPublishState).isEqualTo(1)

    val hasCrimeBatchId = jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'crime_batch_ingestion_attempt' AND column_name = 'crime_batch_id'",
      Int::class.java,
    )
    assertThat(hasCrimeBatchId).isEqualTo(1)
  }

  @Test
  fun `it deduplicates pre-existing attempts in v10 and then enforces source uniqueness`() {
    val seeded = seedDuplicateAttempts()

    flyway().migrate()

    val survivingIds = jdbcTemplate.queryForList(
      """
      SELECT id
      FROM crime_batch_ingestion_attempt
      WHERE bucket = ? AND object_name = ?
      """.trimIndent(),
      UUID::class.java,
      seeded.sharedBucket,
      seeded.sharedObjectName,
    )
    assertThat(survivingIds).containsExactly(seeded.keptAttemptId)

    val dedupAuditRows = jdbcTemplate.queryForObject(
      """
      SELECT COUNT(*)
      FROM crime_batch_ingestion_attempt_dedup_audit
      WHERE kept_id = ? AND removed_id = ? AND bucket = ? AND object_name = ?
      """.trimIndent(),
      Int::class.java,
      seeded.keptAttemptId,
      seeded.removedAttemptId,
      seeded.sharedBucket,
      seeded.sharedObjectName,
    )
    assertThat(dedupAuditRows).isEqualTo(1)

    val removedEmailStillExists = jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM crime_batch_email WHERE id = ?",
      Int::class.java,
      seeded.removedEmailId,
    )
    assertThat(removedEmailStillExists).isZero()

    val removedAttachmentStillExists = jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM crime_batch_email_attachment WHERE id = ?",
      Int::class.java,
      seeded.removedAttachmentId,
    )
    assertThat(removedAttachmentStillExists).isZero()

    val removedBatchStillExists = jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM crime_batch WHERE id = ?",
      Int::class.java,
      seeded.removedBatchId,
    )
    assertThat(removedBatchStillExists).isZero()

    val removedIngestionErrorStillExists = jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM crime_batch_ingestion_error WHERE crime_batch_ingestion_attempt_id = ?",
      Int::class.java,
      seeded.removedEmailId,
    )
    assertThat(removedIngestionErrorStillExists).isZero()

    val removedAttachmentErrorStillExists = jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM crime_batch_email_attachment_ingestion_error WHERE crime_batch_email_attachment_id = ?",
      Int::class.java,
      seeded.removedAttachmentId,
    )
    assertThat(removedAttachmentErrorStillExists).isZero()

    val duplicateInsert = {
      jdbcTemplate.update(
        """
        INSERT INTO crime_batch_ingestion_attempt (id, bucket, object_name, created_at)
        VALUES (?, ?, ?, ?)
        """.trimIndent(),
        UUID.randomUUID(),
        seeded.sharedBucket,
        seeded.sharedObjectName,
        Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 11, 0, 0)),
      )
    }
    org.junit.jupiter.api.assertThrows<DataIntegrityViolationException> { duplicateInsert() }
  }

  private fun seedDuplicateAttempts(): SeededDuplicateAttempts {
    val sharedBucket = "emails"
    val sharedObjectName = "same-object"

    val keptAttemptId = UUID.randomUUID()
    val keptEmailId = UUID.randomUUID()
    val keptAttachmentId = UUID.randomUUID()
    val keptBatchId = UUID.randomUUID()

    val removedAttemptId = UUID.randomUUID()
    val removedEmailId = UUID.randomUUID()
    val removedAttachmentId = UUID.randomUUID()
    val removedBatchId = UUID.randomUUID()

    jdbcTemplate.update(
      """
      INSERT INTO crime_batch_ingestion_attempt (id, bucket, object_name, created_at)
      VALUES (?, ?, ?, ?)
      """.trimIndent(),
      keptAttemptId,
      sharedBucket,
      sharedObjectName,
      Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 9, 0, 0)),
    )
    jdbcTemplate.update(
      """
      INSERT INTO crime_batch_email (id, crime_batch_ingestion_attempt_id, sender, original_sender, subject, sent_at)
      VALUES (?, ?, ?, ?, ?, ?)
      """.trimIndent(),
      keptEmailId,
      keptAttemptId,
      "sender@justice.gov.uk",
      "original@justice.gov.uk",
      "subject-keep",
      Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 9, 0, 0)),
    )
    jdbcTemplate.update(
      """
      INSERT INTO crime_batch_email_attachment (id, crime_batch_email_id, file_name, row_count)
      VALUES (?, ?, ?, ?)
      """.trimIndent(),
      keptAttachmentId,
      keptEmailId,
      "keep.csv",
      1,
    )
    jdbcTemplate.update(
      """
      INSERT INTO crime_batch (id, batch_id, crime_batch_email_attachment_id, created_at)
      VALUES (?, ?, ?, ?)
      """.trimIndent(),
      keptBatchId,
      "batch-keep",
      keptAttachmentId,
      Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 9, 1, 0)),
    )

    jdbcTemplate.update(
      """
      INSERT INTO crime_batch_ingestion_attempt (id, bucket, object_name, created_at)
      VALUES (?, ?, ?, ?)
      """.trimIndent(),
      removedAttemptId,
      sharedBucket,
      sharedObjectName,
      Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 10, 0, 0)),
    )
    jdbcTemplate.update(
      """
      INSERT INTO crime_batch_email (id, crime_batch_ingestion_attempt_id, sender, original_sender, subject, sent_at)
      VALUES (?, ?, ?, ?, ?, ?)
      """.trimIndent(),
      removedEmailId,
      removedAttemptId,
      "sender@justice.gov.uk",
      "original@justice.gov.uk",
      "subject-remove",
      Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 10, 0, 0)),
    )
    jdbcTemplate.update(
      """
      INSERT INTO crime_batch_email_attachment (id, crime_batch_email_id, file_name, row_count)
      VALUES (?, ?, ?, ?)
      """.trimIndent(),
      removedAttachmentId,
      removedEmailId,
      "remove.csv",
      1,
    )
    jdbcTemplate.update(
      """
      INSERT INTO crime_batch (id, batch_id, crime_batch_email_attachment_id, created_at)
      VALUES (?, ?, ?, ?)
      """.trimIndent(),
      removedBatchId,
      "batch-remove",
      removedAttachmentId,
      Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 10, 1, 0)),
    )

    jdbcTemplate.update(
      """
      INSERT INTO crime_batch_ingestion_error (id, error_type, crime_batch_ingestion_attempt_id)
      VALUES (?, ?, ?)
      """.trimIndent(),
      UUID.randomUUID(),
      "EMAIL_ATTACHMENT_NOT_FOUND",
      removedEmailId,
    )

    jdbcTemplate.update(
      """
      INSERT INTO crime_batch_email_attachment_ingestion_error
      (id, crime_batch_email_attachment_id, row_number, crime_reference, error_type, field_name, value)
      VALUES (?, ?, ?, ?, ?, ?, ?)
      """.trimIndent(),
      UUID.randomUUID(),
      removedAttachmentId,
      1L,
      "CRI0001",
      "INVALID_VALUE",
      "crime_reference",
      "bad",
    )

    return SeededDuplicateAttempts(
      sharedBucket = sharedBucket,
      sharedObjectName = sharedObjectName,
      keptAttemptId = keptAttemptId,
      removedAttemptId = removedAttemptId,
      removedEmailId = removedEmailId,
      removedAttachmentId = removedAttachmentId,
      removedBatchId = removedBatchId,
    )
  }

  private fun flyway(targetVersion: String? = null): Flyway {
    val configuration = Flyway.configure()
      .dataSource(postgresContainer.jdbcUrl, postgresContainer.username, postgresContainer.password)
      .locations("classpath:db/migration")
      .cleanDisabled(false)

    if (targetVersion != null) {
      configuration.target(targetVersion)
    }

    return configuration.load()
  }
}

private data class SeededDuplicateAttempts(
  val sharedBucket: String,
  val sharedObjectName: String,
  val keptAttemptId: UUID,
  val removedAttemptId: UUID,
  val removedEmailId: UUID,
  val removedAttachmentId: UUID,
  val removedBatchId: UUID,
)
