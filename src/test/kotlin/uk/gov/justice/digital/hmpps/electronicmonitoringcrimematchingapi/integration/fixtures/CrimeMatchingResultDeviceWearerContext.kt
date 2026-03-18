package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultDeviceWearer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultPosition
import java.time.LocalDateTime

class CrimeMatchingResultDeviceWearerContext(
  private val crimeMatchingResultDeviceWearer: CrimeMatchingResultDeviceWearer,
) {
  fun withPosition() {
    crimeMatchingResultDeviceWearer.positions.add(
      CrimeMatchingResultPosition(
        crimeMatchingResultDeviceWearer = crimeMatchingResultDeviceWearer,
        latitude = 10.0,
        longitude = 10.0,
        capturedDateTime = LocalDateTime.of(2025, 1, 1, 0, 0),
        sequenceLabel = "A1",
        confidenceCircle = 10,
      ),
    )
  }
}
