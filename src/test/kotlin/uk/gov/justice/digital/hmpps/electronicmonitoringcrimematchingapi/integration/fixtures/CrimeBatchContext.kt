package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.Crime
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeBatch
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.CrimeType
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums.PoliceForce
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeBatch.CrimeVersionRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.repository.crimeMatching.CrimeMatchingRunRepository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
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
    crimeDateTimeFrom: Instant = LocalDateTime.of(2025, 1, 1, 0, 0).toInstant(ZoneOffset.UTC),
    crimeDateTimeTo: Instant = LocalDateTime.of(2025, 1, 1, 1, 0).toInstant(ZoneOffset.UTC),
    latitude: Double? = 0.0,
    longitude: Double? = 0.0,
    easting: Double? = null,
    northing: Double? = null,
    crimeText: String = "text",
    block: CrimeContext.() -> Unit,
  ) {
    // Reuse existing crime if it already exists (prevents unique constraint violation)
    val crime =
      crimeRepository
        .findByCrimeReferenceAndPoliceForceArea(crimeRef, policeForce)
        .orElseGet {
          crimeRepository.save(
            Crime(
              policeForceArea = policeForce,
              crimeReference = crimeRef,
            ),
          )
        }

    val version = crimeVersionRepository.save(
      CrimeVersion(
        id = id,
        crime = crime,
        crimeTypeId = crimeType,
        crimeDateTimeFrom = crimeDateTimeFrom,
        crimeDateTimeTo = crimeDateTimeTo,
        latitude = latitude,
        longitude = longitude,
        easting = easting,
        northing = northing,
        crimeText = crimeText,
      ),
    )

    batch.crimeVersions.add(version)

    CrimeContext(
      batch = batch,
      version = version,
      crimeMatchingRunRepository = crimeMatchingRunRepository,
    ).block()
  }
}
