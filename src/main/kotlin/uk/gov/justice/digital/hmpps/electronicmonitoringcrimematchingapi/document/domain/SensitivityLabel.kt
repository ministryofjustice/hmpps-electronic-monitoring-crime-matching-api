package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.document.domain

enum class SensitivityLabel(
  val displayName: String,
  val labelId: String,
) {
  OFFICIAL_SENSITIVE(
    "OFFICIAL - SENSITIVE",
    "74082994-c1b7-400b-ad85-6ab70460486c",
  ),
}
