package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain.contextualreport

data class AuthorisingManagerDetails(
  val name: String,
  val occupation: String,
  val signatureImage: ByteArray? = null,
) {
  init {
    require(name.isNotBlank()) {
      "'name' cannot be blank"
    }

    require(occupation.isNotBlank()) {
      "'occupation' cannot be blank"
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is AuthorisingManagerDetails) return false

    return name == other.name &&
      occupation == other.occupation &&
      signatureImage.contentEquals(other.signatureImage)
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + occupation.hashCode()
    result = 31 * result + signatureImage.contentHashCode()
    return result
  }
}
