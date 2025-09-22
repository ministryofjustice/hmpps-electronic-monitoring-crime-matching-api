package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.DeviceActivation

data class DeviceActivationDto(
  val deviceActivationId: Long,
  val deviceId: Long,
  val deviceName: String,
  val personId: Long,
  val deviceActivationDate: String,
  val deviceDeactivationDate: String?,
  val orderStart: String,
  val orderEnd: String,
) {
  constructor(deviceActivation: DeviceActivation) : this(
    deviceActivationId = deviceActivation.deviceActivationId,
    deviceId = deviceActivation.deviceId,
    deviceName = deviceActivation.deviceName,
    personId = deviceActivation.personId,
    deviceActivationDate = deviceActivation.deviceActivationDate.toString(),
    deviceDeactivationDate = deviceActivation.deviceDeactivationDate.toString(),
    orderStart = deviceActivation.orderStart,
    orderEnd = deviceActivation.orderEnd,
  )
}
