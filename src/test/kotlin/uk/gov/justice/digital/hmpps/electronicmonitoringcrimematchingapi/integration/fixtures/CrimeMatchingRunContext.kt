package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.integration.fixtures

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResult
import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeMatchingResultDeviceWearer

class CrimeMatchingRunContext(
  private val result: CrimeMatchingResult,
) {
  fun withMatchedDeviceWearer(
    deviceId: Long,
    name: String = "name",
    nomisId: String = "nomisId",
    block: CrimeMatchingResultDeviceWearerContext.() -> Unit = {},
  ) {
    val deviceWearer = CrimeMatchingResultDeviceWearer(
      crimeMatchingResult = result,
      deviceId = deviceId,
      name = name,
      nomisId = nomisId,
    )

    CrimeMatchingResultDeviceWearerContext(deviceWearer).block()

    result.deviceWearers.add(
      deviceWearer,
    )
  }
}
