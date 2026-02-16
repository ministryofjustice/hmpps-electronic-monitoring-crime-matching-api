package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class PoliceForce(val value: String, val code: String) {
  AVON_AND_SOMERSET("Avon and Somerset", "AVS"),
  BEDFORDSHIRE("Bedfordshire", "BFD"),
  CHESHIRE("Cheshire", "CHS"),
  CITY_OF_LONDON("City of London", "CoLP"),
  CUMBRIA("Cumbria", "CMB"),
  DERBYSHIRE("Derbyshire", "DBY"),
  DURHAM("Durham", "DUR"),
  ESSEX("Essex", "ESX"),
  GLOUCESTERSHIRE("Gloucestershire", "GLC"),
  GWENT("Gwent", "GWP"),
  HAMPSHIRE("Hampshire", "HAM"),
  HERTFORDSHIRE("Hertfordshire", "HRT"),
  HUMBERSIDE("Humberside", "HMB"),
  KENT("Kent", "KNT"),
  METROPOLITAN("Metropolitan", "MPS"),
  NORTH_WALES("North Wales", "NWL"),
  NOTTINGHAMSHIRE("Nottinghamshire", "NOT"),
  SUSSEX("Sussex", "SXP"),
  WEST_MIDLANDS("West Midlands", "WMP"),
  ;

  companion object {
    fun from(value: String): PoliceForce? = entries.firstOrNull {
      it.value == value
    }
  }
}
