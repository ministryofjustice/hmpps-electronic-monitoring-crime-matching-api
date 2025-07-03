package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.subject

data class SubjectsQueryCriteria(
  val name: String? = null,
  val nomisId: String? = null,
) {
  fun isValid(): Boolean = !(name.isNullOrBlank() && nomisId.isNullOrBlank())
}
