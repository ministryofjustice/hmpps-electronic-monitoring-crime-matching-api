package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.expressions

import uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders.Expression

class CurrentDate : Expression() {
  override fun parameters() = emptyList<String>()

  override fun toString() = "current_date"
}
