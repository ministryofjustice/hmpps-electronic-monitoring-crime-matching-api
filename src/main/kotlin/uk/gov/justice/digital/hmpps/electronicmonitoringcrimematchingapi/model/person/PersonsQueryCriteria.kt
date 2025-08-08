package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.person


class PersonsQueryCriteria(
  val name: String? = null,
  val nomisId: String? = null,
  val includeDeviceActivations: Boolean = false,
  //TODO Pagination
) {
  fun isValid(): Boolean = !(name.isNullOrBlank() && nomisId.isNullOrBlank())
}