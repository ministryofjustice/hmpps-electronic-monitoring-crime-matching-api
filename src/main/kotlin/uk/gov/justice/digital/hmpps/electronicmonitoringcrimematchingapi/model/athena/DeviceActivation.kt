package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.athena

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Table

object DeviceActivation : Table(name = "device_activation") {
  val deviceActivationId = long("device_activation_id")
  val deviceId = long("device_id")
  val personId = long("person_id")
  val deviceActivationDate = date("device_activation_date")
  val deviceDeactivationDate = date("device_deactivation_date")
}
