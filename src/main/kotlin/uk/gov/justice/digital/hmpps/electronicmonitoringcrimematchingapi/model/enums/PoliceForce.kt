package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class PoliceForce(val value: String) {
  AVON_AND_SOMERSET("Avon and Somerset"),
  BEDFORDSHIRE("Bedfordshire"),
  CHESHIRE("Cheshire"),
  CITY_OF_LONDON("City of London"),
  CUMBRIA("Cumbria"),
  DERBYSHIRE("Derbyshire"),
  DURHAM("Durham"),
  ESSEX("Essex"),
  GLOUCESTERSHIRE("Gloucestershire"),
  GWENT("Gwent"),
  HAMPSHIRE("Hampshire"),
  HERTFORDSHIRE("Hertfordshire"),
  HUMBERSIDE("Humberside"),
  KENT("Kent"),
  METROPOLITAN("Metropolitan"),
  NORTH_WALES("North Wales"),
  NOTTINGHAMSHIRE("Nottinghamshire"),
  WEST_MIDLANDS("West Midlands"),
  ;

  companion object {
    fun from(value: String): PoliceForce? = entries.firstOrNull {
      it.value == value
    }
  }
}
