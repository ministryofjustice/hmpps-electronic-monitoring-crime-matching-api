package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

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
import java.util.Date

class CrimeMatchingFixtures(
  private val crimeRepository: CrimeRepository,
  private val crimeVersionRepository: CrimeVersionRepository,
  private val crimeBatchRepository: CrimeBatchRepository,
  private val crimeBatchIngestionAttemptRepository: CrimeBatchIngestionAttemptRepository,
  private val crimeMatchingRunRepository: CrimeMatchingRunRepository,
) {
  fun givenBatch(
    batchId: String,
    policeForce: PoliceForce = PoliceForce.METROPOLITAN,
    block: CrimeBatchContext.() -> Unit,
  ): CrimeBatch {
    val ingestionAttempt = CrimeBatchIngestionAttempt(
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
      rowCount = 1,
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
      crimeBatchRepository = crimeBatchRepository,
      crimeMatchingRunRepository = crimeMatchingRunRepository,
    ).block()

    return batch
  }
}
