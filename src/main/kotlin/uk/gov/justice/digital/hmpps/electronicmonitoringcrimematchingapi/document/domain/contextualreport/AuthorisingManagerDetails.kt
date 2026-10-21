package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

data class AuthorisingManagerDetails(
  val name: String,
  val occupation: String,
) {
  init {
    require(name.isNotBlank()) {
      "'name' cannot be blank"
    }

    require(occupation.isNotBlank()) {
      "'occupation' cannot be blank"
    }
  }
}
