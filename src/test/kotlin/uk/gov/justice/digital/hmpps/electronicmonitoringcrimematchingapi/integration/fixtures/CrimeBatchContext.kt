package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingRunRepository
import java.time.LocalDateTime
import java.util.UUID

class CrimeBatchContext(
  private val batch: CrimeBatch,
  private val policeForce: PoliceForce,
  private val crimeRepository: CrimeRepository,
  private val crimeVersionRepository: CrimeVersionRepository,
  private val crimeMatchingRunRepository: CrimeMatchingRunRepository,
) {
  fun withCrime(
    crimeRef: String,
    id: UUID = UUID.randomUUID(),
    crimeType: CrimeType = CrimeType.AB,
    crimeDateTimeFrom: LocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0),
    crimeDateTimeTo: LocalDateTime = LocalDateTime.of(2025, 1, 1, 1, 0),
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    crimeText: String = "text",
    block: CrimeContext.() -> Unit,
  ) {
    val crime = crimeRepository.save(
      Crime(
        policeForceArea = policeForce,
        crimeReference = crimeRef,
      ),
    )

    val version = crimeVersionRepository.save(
      CrimeVersion(
        id = id,
        crime = crime,
        crimeTypeId = crimeType,
        crimeDateTimeFrom = crimeDateTimeFrom,
        crimeDateTimeTo = crimeDateTimeTo,
        latitude = latitude,
        longitude = longitude,
        easting = null,
        northing = null,
        crimeText = crimeText,
      ),
    )
    crimeRepository.save(crime)

    batch.crimeVersions.add(version)

    CrimeContext(
      batch = batch,
      version = version,
      crimeMatchingRunRepository = crimeMatchingRunRepository,
    ).block()
  }
}