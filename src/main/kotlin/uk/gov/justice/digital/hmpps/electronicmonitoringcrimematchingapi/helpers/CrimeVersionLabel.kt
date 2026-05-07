package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.model.entity.CrimeVersion

fun computeVersionLabel(
  isLatest: Boolean,
  versions: List<CrimeVersion>,
  current: CrimeVersion,
): String {
  val currentVersionIndex = versions.indexOf(current)

  val versionNumber = versions
    .take(currentVersionIndex + 1)
    .count { it.updates.isNotEmpty() } + 1

  val previousVersionNumber = if (currentVersionIndex > 0) {
    versions
      .take(currentVersionIndex)
      .count { it.updates.isNotEmpty() } + 1
  } else {
    null
  }

  val isDuplicate = previousVersionNumber != null && versionNumber == previousVersionNumber

  return if (isLatest) {
    buildString {
      append("Latest version")
      if (isDuplicate) append(" (Duplicate)")
    }
  } else {
    buildString {
      append("Version $versionNumber")
      if (isDuplicate) append(" (Duplicate)")
    }
  }
}
