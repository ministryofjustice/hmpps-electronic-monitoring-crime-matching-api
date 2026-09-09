package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultDeviceWearer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultPosition
import java.time.Instant

class CrimeMatchingResultDeviceWearerContext(
  private val crimeMatchingResultDeviceWearer: CrimeMatchingResultDeviceWearer,
) {
  fun withPosition(
    capturedDateTime: Instant = Instant.parse("2025-01-01T00:00:00Z"),
    sequenceLabel: String = "A1",
  ) {
    crimeMatchingResultDeviceWearer.positions.add(
      CrimeMatchingResultPosition(
        crimeMatchingResultDeviceWearer = crimeMatchingResultDeviceWearer,
        capturedDateTime = capturedDateTime,
        direction = 10,
        latitude = 10.0,
        longitude = 10.0,
        precision = 10,
        sequenceLabel = sequenceLabel,
        speed = 10,
      ),
    )
  }
}
