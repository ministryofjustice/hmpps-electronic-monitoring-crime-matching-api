package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.client

enum class AthenaRole (
  val priority: Int,
) {
  ROLE_EM_CRIME_MATCHING_GENERAL_RO(
    priority = 10,
  ),
  NONE(
    priority = 0,
  ),
}