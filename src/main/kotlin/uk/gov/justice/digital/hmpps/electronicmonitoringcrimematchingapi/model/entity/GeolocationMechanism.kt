package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity

enum class GeolocationMechanism(val value: Long) {
  GPS(1),
  RF(4),
  LBS(5),
  WIFI(6),
  ;

  companion object {
    fun from(value: Long) = entries.first { it.value == value }
  }
}
