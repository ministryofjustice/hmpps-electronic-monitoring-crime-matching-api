package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import org.springframework.jdbc.core.JdbcTemplate
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmailAttachment
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingRunRepository
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

class CrimeMatchingFixtures(
  private val jdbcTemplate: JdbcTemplate,
  private val crimeRepository: CrimeRepository,
  private val crimeVersionRepository: CrimeVersionRepository,
  private val crimeBatchRepository: CrimeBatchRepository,
  private val crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository,
  private val crimeMatchingRunRepository: CrimeMatchingRunRepository,
) {

  fun deleteAll() {
    jdbcTemplate.execute("TRUNCATE TABLE crime_batch_crime_version RESTART IDENTITY")
    crimeMatchingRunRepository.deleteAll()
    crimeBatchRepository.deleteAll()
    crimeRepository.deleteAll()
    crimeBatchIngestionAttemptRepository.deleteAll()
  }

  fun givenBatch(
    batchId: String,
    ingestionAttemptId: UUID = UUID.randomUUID(),
    ingestionCreatedAt: LocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0),
    rowCount: Int = 1,
    policeForce: PoliceForce = PoliceForce.METROPOLITAN,
    block: CrimeBatchContext.() -> Unit,
  ): CrimeBatch {
    val ingestionAttempt = CrimeBatchIngestionAttempt(
      id = ingestionAttemptId,
      createdAt = ingestionCreatedAt,
      bucket = "bucket",
      objectName = "objectName",
    )

    val email = CrimeBatchEmail(
      crimeBatchIngestionAttempt = ingestionAttempt,
      sender = "sender@test",
      originalSender = "original@test",
      subject = "test",
      sentAt = Date(),
    )
    ingestionAttempt.crimeBatchEmail = email

    val attachment = CrimeBatchEmailAttachment(
      crimeBatchEmail = email,
      fileName = "test.csv",
      rowCount = rowCount,
    )
    email.crimeBatchEmailAttachments.add(attachment)

    val batch = CrimeBatch(
      batchId = batchId,
      crimeBatchEmailAttachment = attachment,
    )
    attachment.crimeBatch = batch

    crimeBatchIngestionAttemptRepository.save(ingestionAttempt)

    CrimeBatchContext(
      batch,
      policeForce,
      crimeRepository = crimeRepository,
      crimeVersionRepository = crimeVersionRepository,
      crimeMatchingRunRepository = crimeMatchingRunRepository,
    ).block()

    return batch
  }
}