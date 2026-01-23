package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.service.crimeMatching

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeMatchingRunDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeMatchingResultDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeMatchingResultDeviceWearerDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto.CrimeMatchingResultPositionDto
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingRun
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultDeviceWearer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultPosition
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeBatchRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingRunRepository
import java.time.LocalDateTime
import java.util.UUID

@Service
class CrimeMatchingRunService(
  private val crimeMatchingRunRepository: CrimeMatchingRunRepository,
  private val crimeBatchRepository: CrimeBatchRepository,
  private val crimeVersionRepository: CrimeVersionRepository,
) {

  @Transactional
  fun createCrimeMatchingRun(runDto: CrimeMatchingRunDto): CrimeMatchingRun {
    validateMatchingWindow(runDto.matchingStarted, runDto.matchingEnded)

    val crimeBatch = findCrimeBatch(runDto.crimeBatchId)

    // Create a new Run
    val run = CrimeMatchingRun(
      crimeBatch = crimeBatch,
      algorithmVersion = runDto.algorithmVersion,
      triggerType = runDto.triggerType,
      status = runDto.status,
      matchingStarted = runDto.matchingStarted,
      matchingEnded = runDto.matchingEnded,
      createdAt = LocalDateTime.now(),
    )

    for (resultDto in runDto.results) {
      val crimeVersion = findCrimeVersion(resultDto.crimeVersionId)
      val result = createResult(run, crimeVersion, resultDto)
      run.results.add(result)
    }

    return crimeMatchingRunRepository.save(run)
  }

  // Ensure matchingEnded date is the same or after matchingStarted (Not sure if this is likely but just in case)
  private fun validateMatchingWindow(started: LocalDateTime, ended: LocalDateTime) {
    require(!ended.isBefore(started)) {
      "matchingEnded must be the same as or after matchingStarted"
    }
  }

  private fun findCrimeBatch(id: UUID): CrimeBatch =
    crimeBatchRepository.findById(id)
      .orElseThrow { EntityNotFoundException("No crime batch found with id: $id") }

  private fun findCrimeVersion(id: UUID): CrimeVersion =
    crimeVersionRepository.findById(id)
      .orElseThrow { EntityNotFoundException("No crime version found with id: $id") }

  private fun createResult(run: CrimeMatchingRun, crimeVersion: CrimeVersion, resultDto: CrimeMatchingResultDto): CrimeMatchingResult {
    val result = CrimeMatchingResult(
      crimeVersion = crimeVersion,
      crimeMatchingRun = run,
      createdAt = LocalDateTime.now(),
    )

    for (wearerDto in resultDto.deviceWearers) {
      val wearer = createDeviceWearer(result, wearerDto)
      result.deviceWearers.add(wearer)
    }

    return result
  }

  private fun createDeviceWearer(result: CrimeMatchingResult, wearerDto: CrimeMatchingResultDeviceWearerDto): CrimeMatchingResultDeviceWearer {
    val wearer = CrimeMatchingResultDeviceWearer(
      crimeMatchingResult = result,
      deviceId = wearerDto.deviceId,
      name = wearerDto.name,
      nomisId = wearerDto.nomisId,
    )

    for (positionDto in wearerDto.positions) {
      val position = createPosition(wearer, positionDto)
      wearer.positions.add(position)
    }

    return wearer
  }

  private fun createPosition(wearer: CrimeMatchingResultDeviceWearer, positionDto: CrimeMatchingResultPositionDto): CrimeMatchingResultPosition =
    CrimeMatchingResultPosition(
      crimeMatchingResultDeviceWearer = wearer,
      latitude = positionDto.latitude,
      longitude = positionDto.longitude,
      capturedDateTime = positionDto.capturedDateTime,
      sequenceLabel = positionDto.sequenceLabel,
      confidenceCircle = positionDto.confidenceCircle,
    )
}
