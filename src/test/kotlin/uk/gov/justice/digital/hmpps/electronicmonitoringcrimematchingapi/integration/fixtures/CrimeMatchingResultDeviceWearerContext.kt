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
