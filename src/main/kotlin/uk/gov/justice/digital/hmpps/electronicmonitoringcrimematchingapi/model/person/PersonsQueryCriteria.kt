package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person

data class PersonsQueryCriteria(
  val personName: String? = null,
  val nomisId: String? = null,
  val deviceId: String? = null,
  val includeDeviceActivations: Boolean = false,
) {
  fun isValid(): Boolean {
    if (personName.isNullOrBlank() && nomisId.isNullOrBlank() && deviceId.isNullOrBlank()) {
      return false
    }

    if (!deviceId.isNullOrBlank()) {
      return includeDeviceActivations
    }

    return true
  }
}
