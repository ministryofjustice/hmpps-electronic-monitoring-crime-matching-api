package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class PoliceForce(val identifier: String, val label: String, val code: String) {
  AVON_AND_SOMERSET(
    "AvonSomerset",
    "Avon and Somerset",
    "AVS",
  ),
  BEDFORDSHIRE(
    "Bedfordshire",
    "Bedfordshire",
    "BFD",
  ),
  CHESHIRE(
    "Cheshire",
    "Cheshire",
    "CHS",
  ),
  CITY_OF_LONDON(
    "CoLP",
    "City of London",
    "CoLP",
  ),
  CUMBRIA(
    "Cumbria",
    "Cumbria",
    "CMB",
  ),
  DERBYSHIRE(
    "Derbyshire",
    "Derbyshire",
    "DBY",
  ),
  DURHAM(
    "Durham",
    "Durham",
    "DUR",
  ),
  ESSEX(
    "Essex",
    "Essex",
    "ESX",
  ),
  GLOUCESTERSHIRE(
    "Gloucestershire",
    "Gloucestershire",
    "GLC",
  ),
  GWENT(
    "Gwent",
    "Gwent",
    "GWP",
  ),
  HAMPSHIRE(
    "Hampshire",
    "Hampshire",
    "HAM",
  ),
  HERTFORDSHIRE(
    "Hertfordshire",
    "Hertfordshire",
    "HRT",
  ),
  HUMBERSIDE(
    "Humberside",
    "Humberside",
    "HMB",
  ),
  KENT(
    "Kent",
    "Kent",
    "KNT",
  ),
  METROPOLITAN(
    "Metropolitan",
    "Metropolitan",
    "MPS",
  ),
  NORTH_WALES(
    "NorthWales",
    "North Wales",
    "NWL",
  ),
  NOTTINGHAMSHIRE(
    "Nottinghamshire",
    "Nottinghamshire",
    "NOT",
  ),
  SUSSEX(
    "Sussex",
    "Sussex",
    "SXP",
  ),
  WEST_MIDLANDS(
    "WestMidlands",
    "West Midlands",
    "WMP",
  ),
  ;

  companion object {
    fun from(identifier: String): PoliceForce = entries.first {
      it.identifier == identifier
    }
  }
}
