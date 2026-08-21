package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

class OrderingTerm(val column: Column<*>, val direction: SortDirection) {
  override fun toString(): String = "$column $direction"
}
