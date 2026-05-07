package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultDeviceWearer
import java.time.LocalDateTime

class CrimeMatchingRunContext(
  private val result: CrimeMatchingResult,
) {
  fun withMatchedDeviceWearer(
    address: String = "address",
    deviceId: Long,
    deviceName: String = "deviceName",
    dateOfBirth: LocalDateTime = LocalDateTime.of(2025, 1, 1, 1, 1),
    identifier: String = "1",
    name: String = "name",
    nomisId: String = "nomisId",
    pncRef: String = "pncRef",
    block: CrimeMatchingResultDeviceWearerContext.() -> Unit = {},
  ) {
    val deviceWearer = CrimeMatchingResultDeviceWearer(
      crimeMatchingResult = result,
      address = address,
      dateOfBirth = dateOfBirth,
      deviceId = deviceId,
      deviceName = deviceName,
      identifier = identifier,
      name = name,
      nomisId = nomisId,
      pncRef = pncRef,
    )

    CrimeMatchingResultDeviceWearerContext(deviceWearer).block()

    result.deviceWearers.add(
      deviceWearer,
    )
  }
}
