package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingRun
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersionUpdate
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeMatchingStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeMatchingTriggerType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeVersionFieldName
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingRunRepository
import java.time.LocalDateTime
import java.util.UUID

class CrimeContext(
  private val batch: CrimeBatch,
  private val version: CrimeVersion,
  private val crimeMatchingRunRepository: CrimeMatchingRunRepository,
) {
  fun withMatchingRun(
    algorithmVersion: String = "v1",
    triggerType: CrimeMatchingTriggerType = CrimeMatchingTriggerType.AUTO,
    status: CrimeMatchingStatus = CrimeMatchingStatus.SUCCESS,
    matchingStarted: LocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0),
    matchingEnded: LocalDateTime = LocalDateTime.of(2025, 1, 1, 1, 0),
    matchingResultId: UUID = UUID.randomUUID(),
    block: (CrimeMatchingRunContext.() -> Unit)? = null,
  ) {
    val run = CrimeMatchingRun(
      crimeBatch = batch,
      algorithmVersion = algorithmVersion,
      triggerType = triggerType,
      status = status,
      matchingStarted = matchingStarted,
      matchingEnded = matchingEnded,
    )

    val result = CrimeMatchingResult(
      id = matchingResultId,
      crimeVersion = version,
      crimeMatchingRun = run,
      createdAt = LocalDateTime.of(2025, 1, 1, 1, 0),
    )

    run.results.add(result)

    if (block != null) {
      CrimeMatchingRunContext(result).block()
    }

    crimeMatchingRunRepository.save(run)
  }

  fun withCrimeVersionUpdate(
    fieldName: CrimeVersionFieldName = CrimeVersionFieldName.CRIME_TYPE_ID,
  ) {
    val update = CrimeVersionUpdate(
      crimeVersion = version,
      fieldName = fieldName,
    )

    version.updates.add(update)
  }
}
