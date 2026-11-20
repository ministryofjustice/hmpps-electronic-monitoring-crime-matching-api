package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class CrimeType(val value: String) {
  AB("Aggravated Burglary"),
  BIAD("Burglary in a dwelling"),
  BOTD("Burglary in a building other than a dwelling"),
  RB("Robbery"),
  TFMV("Theft from vehicle"),
  TFP("Theft from the person of another"),
  TOMV("Theft of a vehicle"),
  ;

  companion object {
    fun from(value: String?): CrimeType = CrimeType.entries.first {
      it.name == value
    }
  }
}
