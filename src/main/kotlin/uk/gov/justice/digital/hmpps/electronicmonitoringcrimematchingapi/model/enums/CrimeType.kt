package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class CrimeType(val value: String) {
  RB("Robbery"),
  BIAD("Burglary in a dwelling"),
  AB("Aggravated Burglary"),
  BOTD("Burglary in a building other than a dwelling"),
  TOMV("Theft of a vehicle"),
  TFP("Theft from the person of another"),
  TFMV("Theft from vehicle"),
  ;

  companion object {
    fun from(value: String?): CrimeType = CrimeType.entries.first {
      it.name == value
    }
  }
}
