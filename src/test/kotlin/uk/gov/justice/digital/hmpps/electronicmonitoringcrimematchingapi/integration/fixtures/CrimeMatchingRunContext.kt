package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultDeviceWearer
import java.time.LocalDateTime
import java.util.UUID

class CrimeMatchingRunContext(
  private val result: CrimeMatchingResult,
) {
  fun withMatchedDeviceWearer(
    id: UUID = UUID.randomUUID(),
    address: String = "address",
    deviceId: Long,
    deviceSerialNumber: String = "123456789",
    deviceName: String = "deviceName",
    dateOfBirth: LocalDateTime? = LocalDateTime.of(2025, 1, 1, 1, 1),
    identifier: String = "DEVWR0000001",
    name: String = "name",
    nomisId: String = "nomisId",
    pncRef: String = "pncRef",
    block: CrimeMatchingResultDeviceWearerContext.() -> Unit = {},
  ) {
    val deviceWearer = CrimeMatchingResultDeviceWearer(
      id = id,
      crimeMatchingResult = result,
      address = address,
      dateOfBirth = dateOfBirth,
      deviceId = deviceId,
      deviceSerialNumber = deviceSerialNumber,
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
