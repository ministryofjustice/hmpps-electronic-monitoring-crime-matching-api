package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.enums

enum class CrimeMatchingTriggerType(val value: String) {
  AUTO("AUTO"),
  MANUAL("MANUAL"),
}

enum class CrimeMatchingStatus(val value: String) {
  SUCCESS("Success"),
  FAILURE("Failure"),
}
