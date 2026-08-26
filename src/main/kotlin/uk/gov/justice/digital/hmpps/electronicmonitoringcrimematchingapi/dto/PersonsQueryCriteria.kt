package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.dto

data class PersonsQueryCriteria(
  val name: String? = null,
  val nomisId: String? = null,
  val deviceId: String? = null,
) {
  fun isValid(): Boolean = !(name.isNullOrBlank() && nomisId.isNullOrBlank() && deviceId.isNullOrBlank())
}
