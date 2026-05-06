package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchEmail
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatchIngestionAttempt
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchIngestionAttemptRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionUpdateRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingRunRepository
import java.time.LocalDateTime
import java.util.*

class CrimeMatchingFixtures(
  private val crimeRepository: CrimeRepository,
  private val crimeVersionRepository: CrimeVersionRepository,
  private val crimeVersionUpdateRepository: CrimeVersionUpdateRepository,
  private val crimeBatchRepository: CrimeBatchRepository,
  private val crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository,
  private val crimeMatchingRunRepository: CrimeMatchingRunRepository,
) {

  fun deleteAll() {
    crimeMatchingRunRepository.deleteAll()
    crimeBatchRepository.deleteAll()
    crimeRepository.deleteAll()
    crimeBatchIngestionAttemptRepository.deleteAll()
  }

  fun givenIngestionAttempt(
    id: UUID = UUID.randomUUID(),
    createdAt: LocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0),
    block: CrimeBatchIngestionAttemptContext.() -> Unit = {},
  ): CrimeBatchIngestionAttempt {
    val ingestionAttempt = CrimeBatchIngestionAttempt(
      id = id,
      createdAt = createdAt,
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

    CrimeBatchIngestionAttemptContext(
      crimeBatchEmail = email,
    ).block()

    crimeBatchIngestionAttemptRepository.save(ingestionAttempt)

    return ingestionAttempt
  }

  fun givenBatch(
    crimeBatchId: UUID = UUID.randomUUID(),
    batchId: String = "batch1",
    policeForce: PoliceForce = PoliceForce.METROPOLITAN,
    ingestionCreatedAt: LocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0),
    ingestionAttempt: CrimeBatchIngestionAttempt = givenIngestionAttempt { withAttachment() },
    block: CrimeBatchContext.() -> Unit = {},
  ): CrimeBatch {
    val attachment = ingestionAttempt.crimeBatchEmail!!.crimeBatchEmailAttachments.first()
    val batch = CrimeBatch(
      id = crimeBatchId,
      batchId = batchId,
      crimeBatchEmailAttachment = attachment,
      createdAt = ingestionCreatedAt,
    )
    attachment.crimeBatch = batch

    crimeBatchIngestionAttemptRepository.save(ingestionAttempt)

    CrimeBatchContext(
      batch,
      policeForce,
      crimeRepository = crimeRepository,
      crimeVersionRepository = crimeVersionRepository,
      crimeVersionUpdateRepository = crimeVersionUpdateRepository,
      crimeMatchingRunRepository = crimeMatchingRunRepository,
    ).block()

    crimeBatchRepository.save(batch)

    return batch
  }
}
