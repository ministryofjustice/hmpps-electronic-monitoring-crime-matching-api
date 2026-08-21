package uk.gov.justice.digital.hmpps.electronicmonitoringcrimematchingapi.helpers.querybuilders

class Ordering {
  private val terms = mutableListOf<OrderingTerm>()

  val Column<*>.asc: Unit
    get() {
      terms += OrderingTerm(this, SortDirection.ASC)
    }

  val Column<*>.desc: Unit
    get() {
      terms += OrderingTerm(this, SortDirection.DESC)
    }

  override fun toString(): String = terms.joinToString(", ")
}
