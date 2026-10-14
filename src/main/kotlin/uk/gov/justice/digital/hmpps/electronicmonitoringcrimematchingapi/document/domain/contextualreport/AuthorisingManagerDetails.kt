package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

data class AuthorisingManagerDetails(
  val address: String,
  val name: String,
  val occupation: String,
) {
  init {
    require(address.isNotBlank()) {
      "'address' cannot be blank"
    }

    require(name.isNotBlank()) {
      "'name' cannot be blank"
    }

    require(occupation.isNotBlank()) {
      "'occupation' cannot be blank"
    }
  }
}
