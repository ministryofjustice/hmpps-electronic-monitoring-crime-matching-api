package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultDeviceWearer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultPosition
import java.time.Instant

class CrimeMatchingResultDeviceWearerContext(
  private val crimeMatchingResultDeviceWearer: CrimeMatchingResultDeviceWearer,
) {
  fun withPosition(
    capturedDateTime: Instant = Instant.parse("2025-01-01T00:00:00Z"),
    caseloadDatetimeAdded: Instant? = Instant.parse("2025-01-01T00:00:01Z"),
    positionDatetimeAdded: Instant? = Instant.parse("2025-01-01T00:00:02Z"),
    sequenceLabel: String = "A1",
    entryBearing: Double? = null,
    exitBearing: Double? = null,
    precision: Long = 10,
  ) {
    crimeMatchingResultDeviceWearer.positions.add(
      CrimeMatchingResultPosition(
        crimeMatchingResultDeviceWearer = crimeMatchingResultDeviceWearer,
        capturedDateTime = capturedDateTime,
        direction = 10,
        entryBearing = entryBearing,
        exitBearing = exitBearing,
        latitude = 10.0,
        longitude = 10.0,
        precision = precision,
        sequenceLabel = sequenceLabel,
        caseloadDatetimeAdded = caseloadDatetimeAdded,
        positionDatetimeAdded = positionDatetimeAdded,
        speed = 10,
      ),
    )
  }
}
