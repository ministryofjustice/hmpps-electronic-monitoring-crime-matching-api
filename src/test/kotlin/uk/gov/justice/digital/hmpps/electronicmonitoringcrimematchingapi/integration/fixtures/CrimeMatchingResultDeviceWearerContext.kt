package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultDeviceWearer
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultPosition
import java.time.LocalDateTime

class CrimeMatchingResultDeviceWearerContext(
  private val crimeMatchingResultDeviceWearer: CrimeMatchingResultDeviceWearer,
) {
  fun withPosition(
    capturedDateTime: LocalDateTime = LocalDateTime.of(2025, 1, 1, 0, 0),
    sequenceLabel: String = "A1",
  ) {
    crimeMatchingResultDeviceWearer.positions.add(
      CrimeMatchingResultPosition(
        crimeMatchingResultDeviceWearer = crimeMatchingResultDeviceWearer,
        latitude = 10.0,
        longitude = 10.0,
        capturedDateTime = capturedDateTime,
        sequenceLabel = sequenceLabel,
        confidenceCircle = 10,
      ),
    )
  }
}
