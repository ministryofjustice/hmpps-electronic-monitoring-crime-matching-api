package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person

data class PersonsQueryCriteria(
  val personName: String? = null,
  val nomisId: String? = null,
  // TODO extra validation on this only being populated when includeDeviceActivations is also true
  val deviceId: String? = null,
  val includeDeviceActivations: Boolean = false,
) {
  fun isValid(): Boolean = !(personName.isNullOrBlank() && nomisId.isNullOrBlank() && deviceId.isNullOrBlank())
}
